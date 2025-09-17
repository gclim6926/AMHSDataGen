async function runAddAddresses() {
    try {
        showLoading();
        const result = await callAPI('/api/run-generate');
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
        const result = await callAPI('/api/run-add-lines');
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
        const result = await callAPI('/api/run-check');
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

// OHT Track 기본값들을 전역으로 정의
const OHT_DEFAULT_VALUES = [
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

// OHT 기본값을 반환하는 함수 (다른 파일에서 사용)
function getOHTDefaultValues() {
    return OHT_DEFAULT_VALUES;
}

// 공통 API 호출 함수
async function callAPI(endpoint, method = 'POST', body = null) {
    const options = {
        method: method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) {
        options.body = JSON.stringify(body);
    }
    
    const response = await fetch(endpoint, options);
    if (!response.ok) {
        throw new Error(`서버 오류 (${response.status})`);
    }
    return await response.json();
}

function showUDPGeneratorForm() {
    showStatus('OHT Track 생성 폼을 표시합니다.', 'info');
    const defaults = OHT_DEFAULT_VALUES;
    const rows = defaults.map((p,i)=>{
      const idx=i;
      return `
        <tr>
          <td style="padding:6px 8px;"><label><input type="checkbox" name="enabled_${idx}" checked> OHT_${idx}</label></td>
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
            </form>
        </div>`;
    
    // 기존 고정 버튼들 제거
    cleanupAllFixedButtons();
    
    // 화면 하단에 고정될 OHT 버튼 생성
    createFixedOhtButton();
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

        // 통합된 단일 엔드포인트로 한 번에 생성 요청
        const bulkPayload = entries.map(e=>({ startAddress: e.start, destinationAddress: e.dest, ohtId: `OHT_${e.idx}` }));
        const bulkRes = await fetch('/api/run-udp-generator', {
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
    
    // 주요 정보 섹션 생성
    let summaryHTML = '';
    if (configUpdated) {
        // add Addresses용 정보
        let addressInfo = '';
        if (configUpdated.generated_addresses !== undefined) {
            addressInfo = `
                <div class="summary-item">
                    <span class="summary-label">생성된 주소 수:</span>
                    <span class="summary-value">${configUpdated.generated_addresses}개</span>
                </div>
                <div class="summary-item">
                    <span class="summary-label">생성된 라인 수:</span>
                    <span class="summary-value">${configUpdated.generated_lines}개</span>
                </div>`;
        }
        
        // add Lines용 정보
        let linesInfo = '';
        if (configUpdated.phase1_added !== undefined) {
            linesInfo = `
                <div class="summary-item">
                    <span class="summary-label">Phase 1 추가 라인:</span>
                    <span class="summary-value">${configUpdated.phase1_added}개</span>
                </div>
                <div class="summary-item">
                    <span class="summary-label">Phase 2 추가 라인:</span>
                    <span class="summary-value">${configUpdated.phase2_added}개</span>
                </div>
                <div class="summary-item">
                    <span class="summary-label">총 라인 수:</span>
                    <span class="summary-value">${configUpdated.total_lines}개</span>
                </div>`;
        }
        
        // add Stations용 정보
        let stationsInfo = '';
        if (configUpdated.generated_stations !== undefined) {
            stationsInfo = `
                <div class="summary-item">
                    <span class="summary-label">생성된 스테이션 수:</span>
                    <span class="summary-value">${configUpdated.generated_stations}개</span>
                </div>`;
        }
        
        // 공통 정보
        let commonInfo = `
            <div class="summary-item">
                <span class="summary-label">데이터베이스 키:</span>
                <span class="summary-value">${configUpdated.database_key || 'N/A'}</span>
            </div>`;
        
        // check Errors용 정보
        let checkInfo = '';
        if (configUpdated.checked_addresses !== undefined) {
            checkInfo = `
                <div class="summary-item">
                    <span class="summary-label">검사완료 주소 수:</span>
                    <span class="summary-value">${configUpdated.checked_addresses}개</span>
                </div>
                <div class="summary-item">
                    <span class="summary-label">검사완료 라인 수:</span>
                    <span class="summary-value">${configUpdated.checked_lines}개</span>
                </div>
                <div class="summary-item">
                    <span class="summary-label">검사완료 스테이션 수:</span>
                    <span class="summary-value">${configUpdated.checked_stations}개</span>
                </div>`;
        }
        
        summaryHTML = `
            <div class="result-section">
                <h4>📊 생성 결과 요약</h4>
                <div class="summary-grid">
                    ${addressInfo}
                    ${linesInfo}
                    ${stationsInfo}
                    ${checkInfo}
                    ${commonInfo}
                </div>
            </div>`;
    }
    
    let outputHTML = `
        <div class="execution-result">
            <h3>🚀 ${viewerName} 실행 결과</h3>
            ${summaryHTML}
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
                                const url = '/viewer3d?layers=' + encodeURIComponent(layer) + '&overlap=0&primary=' + encodeURIComponent(layer) + '&comps=' + encodeURIComponent(comps);
                                window.open(url, '_blank');
                            });
                        }
                    })();
                    break;
                case 'layout_seed': 
                    cleanupAllFixedButtons();
                    runLayoutSeed(); 
                    break;
                case 'add_addresses': 
                    cleanupAllFixedButtons();
                    runAddAddresses(); 
                    break;
                case 'add_lines_endpoint': 
                    cleanupAllFixedButtons();
                    runAddLines(); 
                    break;
                case 'check': 
                    cleanupAllFixedButtons();
                    runCheckPy(); 
                    break;
                case 'stations': 
                    cleanupAllFixedButtons();
                    runStationsPy(); 
                    break;
                case 'udp_generator': showUDPGeneratorForm(); break;
                case 'extract_amhs': showExtractPanel(); break;
            }
        });
    });
});


// 모든 고정 버튼 정리 함수
function cleanupAllFixedButtons() {
    const fixedButtons = document.querySelectorAll('.fixed-button-container');
    fixedButtons.forEach(button => button.remove());
}

// OHT 고정 버튼 생성 함수
function createFixedOhtButton() {
    const fixedButtonContainer = document.createElement('div');
    fixedButtonContainer.className = 'fixed-button-container';
    fixedButtonContainer.id = 'fixed-oht-button';
    
    const submitButton = document.createElement('button');
    submitButton.className = 'fixed-update-btn';
    submitButton.textContent = '🚀 update to oht_track_data.log';
    submitButton.onclick = (e) => {
        e.preventDefault();
        const form = document.getElementById('udpGeneratorForm');
        if (form) {
            const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
            form.dispatchEvent(submitEvent);
        }
    };
    
    fixedButtonContainer.appendChild(submitButton);
    document.body.appendChild(fixedButtonContainer);
}

function showExtractPanel() {
    cleanupAllFixedButtons(); // 기존 고정 버튼들 제거
    
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
                    <button type="submit" class="update-btn">Download Data</button>
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



