async function runAddAddresses() {
    try {
        showLoading();
        const res = await fetch('/api/run-generate', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        const result = await res.json();
        hideLoading();
        if (result.success) {
            showStatus('✅ add Addresses가 성공적으로 실행되었습니다.', 'success');
            if (result.execution_output) displayExecutionOutput('add Addresses', result.execution_output, result.config_updated);
        } else {
            showStatus('❌ add Addresses 실행 실패: ' + result.message, 'error');
        }
    } catch (e) {
        hideLoading();
        showStatus('❌ add Addresses 실행 중 오류 발생: ' + e.message, 'error');
    }
}

async function runAddLines() {
    try {
        showLoading();
        const res = await fetch('/api/run-add-lines', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        const result = await res.json();
        hideLoading();
        if (result.success) {
            showStatus('✅ add Lines가 성공적으로 실행되었습니다.', 'success');
            if (result.execution_output) displayExecutionOutput('add Lines', result.execution_output, result.config_updated);
        } else {
            showStatus('❌ add Lines 실행 실패: ' + result.message, 'error');
        }
    } catch (e) {
        hideLoading();
        showStatus('❌ add Lines 실행 중 오류 발생: ' + e.message, 'error');
    }
}

async function runCheckPy() {
    try {
        showLoading();
        const res = await fetch('/api/run-check', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        const result = await res.json();
        hideLoading();
        if (result.success) {
            showStatus('✅ Checker가 성공적으로 실행되었습니다.', 'success');
            if (result.execution_output) displayExecutionOutput('Checker', result.execution_output, result.config_updated);
        } else {
            showStatus('❌ Checker 실행 실패: ' + result.message, 'error');
        }
    } catch (e) {
        hideLoading();
        showStatus('❌ Checker 실행 중 오류 발생: ' + e.message, 'error');
    }
}

async function runStationsPy() {
    try {
        showLoading();
        const res = await fetch('/api/run-stations', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        const result = await res.json();
        hideLoading();
        if (result.success) {
            showStatus('✅ add Stations가 성공적으로 실행되었습니다.', 'success');
            if (result.execution_output) {
                const resultArea = document.getElementById('resultArea');
                let outputHTML = `
                    <div class="execution-result">
                        <h3>🚀 add Stations 실행 결과</h3>
                        <div class="result-section">
                            <h4>📝 터미널 출력</h4>
                            <div class="terminal-output"><pre>${result.execution_output.terminal_logs || '출력 없음'}</pre></div>
                        </div>
                        <div class="result-section">
                            <h4>🔍 상세 정보</h4>
                            <div class="detail-info">
                                <details><summary>stdout 출력</summary><pre>${result.execution_output.stdout || '출력 없음'}</pre></details>
                                <details><summary>stderr 출력</summary><pre>${result.execution_output.stderr || '출력 없음'}</pre></details>
                            </div>
                        </div>
                    </div>`;
                resultArea.innerHTML = outputHTML;
            }
        } else {
            showStatus('❌ add Stations 실행 실패: ' + result.message, 'error');
        }
    } catch (e) {
        hideLoading();
        showStatus('❌ add Stations 실행 중 오류 발생: ' + e.message, 'error');
    }
}

function showUDPGeneratorForm() {
    showStatus('OHT Track 생성 폼을 표시합니다.', 'info');
    const defaults = [
      [100010,100110],
      [100510,100610],
      [101010,101110],
      [101510,101610],
      [102010,102110],
      [102510,102610],
      [103010,103110],
      [103510,103610],
      [104010,104110],
      [105010,105110]
    ];
    const rows = defaults.map((p,i)=>{
      const idx=i;
      return `
        <tr>
          <td style="padding:6px 8px;"><label><input type="checkbox" name="enabled_${idx}" ${idx===0?'checked':''}> OHT_${idx}</label></td>
          <td style="padding:6px 8px;"><input type="number" name="startAddress_${idx}" class="form-control" value="${p[0]}" placeholder="예: 100050"></td>
          <td style="padding:6px 8px;"><input type="number" name="destinationAddress_${idx}" class="form-control" value="${p[1]}" placeholder="예: 100100"></td>
        </tr>`;
    }).join('');
    document.getElementById('resultArea').innerHTML = `
        <div class="execution-result">
            <h3>🚀 OHT Track 생성</h3>
            <p>여러 OHT에 대해 시작/목적지 주소를 입력하고 OHT 이동 트랙(UDP 로그)을 생성합니다.</p>
            <form id="udpGeneratorForm" onsubmit="runUDPGenerator(event)">
                <table style="width:100%; border-collapse: collapse; margin: 10px 0;">
                  <thead>
                    <tr>
                      <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">OHT_ID</th>
                      <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">시작 주소</th>
                      <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">목적지 주소</th>
                    </tr>
                  </thead>
                  <tbody>
                    ${rows}
                  </tbody>
                </table>
                <div class="form-actions">
                    <button type="submit" class="update-btn">update to DB</button>
                </div>
            </form>
        </div>`;
}

async function runUDPGenerator(event) {
    event.preventDefault();
    try {
        showLoading();
        const form = (event && event.target && event.target.closest && event.target.closest('form'))
            || document.getElementById('udpGeneratorForm')
            || document.querySelector('#resultArea form')
            || document.querySelector('form');
        if (!form) {
            hideLoading();
            showStatus('폼을 찾을 수 없습니다. 페이지를 새로고침한 뒤 다시 시도하세요.', 'error');
            return;
        }
        const entries = [];
        for (let i=0;i<10;i++) {
            const s = form.querySelector(`[name="startAddress_${i}"]`);
            const d = form.querySelector(`[name="destinationAddress_${i}"]`);
            const en = form.querySelector(`[name="enabled_${i}"]`);
            const enabled = en ? en.checked : false;
            const sv = s && s.value ? parseInt(s.value,10) : null;
            const dv = d && d.value ? parseInt(d.value,10) : null;
            if (enabled && Number.isFinite(sv) && Number.isFinite(dv)) entries.push({ idx:i, start:sv, dest:dv });
        }
        if (!entries.length) {
            hideLoading();
            showStatus('입력된 행이 없습니다. 값을 입력해 주세요.', 'error');
            return;
        }

        // Bulk API로 한 번에 생성 요청
        const bulkPayload = entries.map(e=>({ startAddress: e.start, destinationAddress: e.dest, ohtId: `OHT_${e.idx}` }));
        const bulkRes = await fetch('/api/run-udp-generator-bulk', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(bulkPayload)
        });
        const bulkResult = await bulkRes.json();
        hideLoading();
        const resultArea = document.getElementById('resultArea');
        let outputHTML = `
            <div class="execution-result">
                <h3>🚀 OHT Track 생성 결과</h3>
                <div class="result-section">
                    <h4>📊 요약</h4>
                    <div class="terminal-output"><pre>${bulkResult.success ? (bulkResult.execution_output.terminal_logs||'완료') : ('실패: ' + bulkResult.message)}</pre></div>
                </div>
            </div>`;
        resultArea.innerHTML = outputHTML;
    } catch (error) {
        hideLoading();
        showStatus('❌ OHT Track 생성 중 오류 발생: ' + error.message, 'error');
    }
}

function displayExecutionOutput(viewerName, executionOutput, configUpdated) {
    const resultArea = document.getElementById('resultArea');
    let outputHTML = `
        <div class="execution-result">
            <h3>🚀 ${viewerName} 실행 결과</h3>
            <div class="result-section">
                <h4>📝 터미널 출력</h4>
                <div class="terminal-output"><pre>${executionOutput.terminal_logs || '출력 없음'}</pre></div>
            </div>
            <div class="result-section">
                <h4>🔍 상세 정보</h4>
                <div class="detail-info">
                    <details><summary>stdout 출력</summary><pre>${executionOutput.stdout || '출력 없음'}</pre></details>
                    <details><summary>stderr 출력</summary><pre>${executionOutput.stderr || '출력 없음'}</pre></details>
                </div>
            </div>
        </div>`;
    resultArea.innerHTML = outputHTML;
}

// Menu routing
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', function() {
            document.querySelectorAll('.menu-item').forEach(i => i.classList.remove('active'));
            this.classList.add('active');
            const view = this.getAttribute('data-view');
            currentView = view;
            switch(view) {
                case '2d':
                    (function(){
                        const filters = getFilterValues ? getFilterValues() : { layers: [] };
                        const overlap = !!document.querySelector('input[value="Overlap"]:checked');
                        const layers = (filters.layers || []).filter(l => l !== 'Overlap');
                        const comps = (filters.components || []).join(',');
                        if (!layers.length) { window.open('/viewer2d','_blank'); return; }
                        if (overlap) {
                            const url = '/viewer2d?layers=' + encodeURIComponent(layers.join(',')) + '&overlap=1&comps=' + encodeURIComponent(comps);
                            window.open(url, '_blank');
                        } else {
                            layers.forEach(layer => {
                                const url = '/viewer2d?layers=' + encodeURIComponent(layer) + '&overlap=0&comps=' + encodeURIComponent(comps);
                                window.open(url, '_blank');
                            });
                        }
                    })();
                    break;
                case '3d':
                    (function(){
                        const filters = getFilterValues ? getFilterValues() : { layers: [], components: [] };
                        const overlap = !!document.querySelector('input[value="Overlap"]:checked');
                        const layers = (filters.layers || []).filter(l => l !== 'Overlap');
                        const comps = (filters.components || []).join(',');
                        if (!layers.length) { window.open('/viewer3d','_blank'); return; }
                        if (overlap) {
                            const url = '/viewer3d?layers=' + encodeURIComponent(layers.join(',')) + '&overlap=1&comps=' + encodeURIComponent(comps);
                            window.open(url, '_blank');
                        } else {
                            layers.forEach(layer => {
                                const url = '/viewer3d?layers=' + encodeURIComponent(layers.join(',')) + '&overlap=0&primary=' + encodeURIComponent(layer) + '&comps=' + encodeURIComponent(comps);
                                window.open(url, '_blank');
                            });
                        }
                    })();
                    break;
                case 'layout_seed': runLayoutSeed(); break;
                case 'add_addresses': runAddAddresses(); break;
                case 'add_lines_endpoint': runAddLines(); break;
                case 'check': runCheckPy(); break;
                case 'stations': runStationsPy(); break;
                case 'udp_generator': showUDPGeneratorForm(); break;
                case 'extract_amhs': showExtractPanel(); break;
            }
        });
    });
});

async function runExtractAMHS() {
    try {
        showLoading();
        const res = await fetch('/api/extract-amhs-data', { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        const result = await res.json();
        hideLoading();
        if (result.success) {
            showStatus('✅ AMHS_data 추출이 완료되었습니다.', 'success');
            const resultArea = document.getElementById('resultArea');
            resultArea.innerHTML = `
                <div class="execution-result">
                    <h3>📦 Extract 결과</h3>
                    <div class="result-section">
                        <h4>파일 경로</h4>
                        <div class="detail-info">
                            <p><strong>input.json:</strong> ${result.input_path}</p>
                            <p><strong>output.json:</strong> ${result.output_path}</p>
                        </div>
                    </div>
                </div>`;
        } else {
            showStatus('❌ AMHS_data 추출 실패: ' + result.message, 'error');
        }
    } catch (e) {
        hideLoading();
        showStatus('❌ AMHS_data 추출 중 오류: ' + e.message, 'error');
    }
}

async function runDownloadAMHS() {
    try {
        showStatus('AMHS_data 압축 생성 중...', 'info');
        const a = document.createElement('a');
        a.href = '/api/download-amhs-data';
        a.download = 'amhs_data.zip';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        showStatus('✅ amhs_data.zip 다운로드를 시작했습니다.', 'success');
        const resultArea = document.getElementById('resultArea');
        resultArea.innerHTML = `
            <div class="execution-result">
                <h3>📦 AMHS_data 다운로드</h3>
                <p>브라우저가 <code>amhs_data.zip</code> 다운로드를 시작했습니다.</p>
            </div>`;
    } catch (e) {
        showStatus('❌ AMHS_data 다운로드 중 오류: ' + e.message, 'error');
    }
}

function showExtractPanel() {
    const resultArea = document.getElementById('resultArea');
    resultArea.innerHTML = `
        <div class="execution-result">
            <h3>📦 extract DATA</h3>
            <p>다운로드할 항목을 선택하세요.</p>
            <form id="extractForm" onsubmit="submitExtract(event)">
                <label style="display:block;margin:6px 0;"><input type="checkbox" name="input" checked> layout_input.json</label>
                <label style="display:block;margin:6px 0;"><input type="checkbox" name="output" checked> layout_output.json</label>
                <label style="display:block;margin:6px 0;"><input type="checkbox" name="oht_log"> oht_track_data.log</label>
                <div class="form-actions">
                    <button type="submit" class="update-btn">extract DATA</button>
                    <button type="button" class="update-btn" style="background:#17a2b8;" onclick="openH2Console()">h2 Console</button>
                    <button type="button" class="update-btn" style="background:#dc3545;" onclick="resetDb()">DB reset</button>
                </div>
            </form>
        </div>`;
}

async function submitExtract(e) {
    e.preventDefault();
    try {
        showStatus('선택 항목 압축 생성 중...', 'info');
        const form = document.getElementById('extractForm');
        const payload = {
            input: form.querySelector('input[name="input"]').checked,
            output: form.querySelector('input[name="output"]').checked,
            oht_log: form.querySelector('input[name="oht_log"]').checked
        };
        const res = await fetch('/api/download-selected', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
        });
        if (!res.ok) { throw new Error('서버 오류(' + res.status + ')'); }
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = 'selected_data.zip'; document.body.appendChild(a); a.click(); document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        showStatus('✅ selected_data.zip 다운로드를 시작했습니다.', 'success');
    } catch (err) {
        showStatus('❌ 추출 중 오류: ' + err.message, 'error');
    }
}

function openH2Console() {
    try {
        window.open('/h2-console','_blank');
        showStatus('H2 Console을 새 창에서 열었습니다.', 'info');
    } catch (e) {
        showStatus('❌ H2 Console 열기 실패: ' + e.message, 'error');
    }
}

async function resetDb() {
    try {
        if (!confirm('DB의 모든 데이터를 삭제하고 초기화합니다. 계속하시겠습니까?')) return;
        showStatus('DB 리셋 중...', 'info');
        const res = await fetch('/api/reset-db', { method: 'POST' });
        const result = await res.json();
        if (result.success) showStatus('✅ DB가 초기화되었습니다.', 'success');
        else showStatus('❌ DB 초기화 실패: ' + (result.message||'unknown'), 'error');
    } catch (e) {
        showStatus('❌ DB 초기화 중 오류: ' + e.message, 'error');
    }
}


