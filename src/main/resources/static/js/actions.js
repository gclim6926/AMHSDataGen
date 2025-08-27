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
                            <h4>📊 실행 정보</h4>
                            <div class="config-info">
                                <p><strong>스크립트:</strong> ${result.config_updated.script}</p>
                                <p><strong>상태:</strong> ${result.config_updated.status}</p>
                                <p><strong>실행 방법:</strong> ${result.config_updated.method}</p>
                            </div>
                        </div>
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
                    <button type="submit" class="update-btn">OHT Track 생성</button>
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
                <div class="form-actions">
                    <button onclick="showUDPGeneratorForm()" class="update-btn">새로운 OHT Track 생성</button>
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
                <h4>📊 설정 정보</h4>
                <div class="config-info">
                    <p><strong>선택된 레이어:</strong> ${configUpdated.selected_layers ? configUpdated.selected_layers.join(', ') : 'N/A'}</p>
                    <p><strong>시각화 모드:</strong> ${configUpdated.visualization_mode || 'N/A'}</p>
                </div>
            </div>
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
                case 'equipments': showEquipmentsInfo(); break;
            }
        });
    });
});

function showEquipmentsInfo() {
    showStatus('Equipments 정보를 표시합니다.', 'info');
    document.getElementById('resultArea').innerHTML = `
        <h3>Equipments 정보</h3>
        <p>Equipments는 레이아웃의 장비들을 나타냅니다.</p>
        <p>현재는 기본적인 정보만 표시됩니다.</p>
        <p>더 자세한 정보를 보려면 "add Addresses" 메뉴를 선택하세요.</p>`;
}


