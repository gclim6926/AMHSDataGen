async function runLayoutSeed() {
    try {
        showLoading();
        const response = await fetch('/api/get-input-data');
        const result = await response.json();
        if (result.success) {
            showStatus('샘플 데이터를 성공적으로 로드했습니다.', 'success');
            displayInputForm(result.data);
        } else {
            showStatus('샘플 데이터 로드 실패: ' + result.message, 'error');
        }
    } catch (error) {
        showStatus(`샘플 데이터 로드 중 오류가 발생했습니다: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

function displayInputForm(inputData) {
    const resultArea = document.getElementById('resultArea');
    const container = document.createElement('div');
    container.className = 'execution-result';
    const title = document.createElement('h3');
    title.textContent = '🔧 Layout Seed - 샘플 기반 편집';
    container.appendChild(title);
    const description = document.createElement('div');
    description.className = 'layout-description';
    description.innerHTML = `
        <h4>🏗️ AMHS Spine Layout Design</h4>
        <p>AMHS(Automated Material Handling System)의 3층 구조 Spine Layout을 설계합니다. 각 구성 요소의 좌표를 입력하여 OHT(Overhead Hoist Transport) 시스템의 이동 경로를 정의합니다.</p>
        
        <div class="layout-sections">
            <div class="layout-section">
                <h5>🔄 층간 이동 시스템</h5>
                <ul>
                    <li><strong>layout_crossover</strong>: OHT의 층간(z0, z4822, z6022) 이동을 위한 3차원 좌표 트랙</li>
                    <li><strong>z0_4822</strong>: z0 ↔ z4822 층간 이동 전용 포인트</li>
                    <li><strong>z4822_6022</strong>: z4822 ↔ z6022 층간 이동 전용 포인트</li>
                </ul>
            </div>
            
            <div class="layout-section">
                <h5>🏭 z4822층 (중간층) 레일 시스템</h5>
                <ul>
                    <li><strong>z4822</strong>: 중간층의 주요 레일 트랙 (2D 좌표)</li>
                    <li><strong>central_loop</strong>: InterBay, Main Loop - 공장 중앙을 가로지르는 핵심 레일</li>
                    <li><strong>local_loop</strong>: IntraBay - 특정 제조 장비 구역으로 연결되는 분기 레일</li>
                    <li><strong>local_loop_for_layer</strong>: 층간 연결을 위한 버퍼 공간 및 흐름 제어 포인트</li>
                </ul>
            </div>
            
            <div class="layout-section">
                <h5>🏭 z6022층 (최상층) 레일 시스템</h5>
                <ul>
                    <li><strong>z6022</strong>: 최상층의 주요 레일 트랙 (2D 좌표)</li>
                    <li><strong>central_loop</strong>: InterBay, Main Loop - 공장 중앙을 가로지르는 핵심 레일</li>
                    <li><strong>local_loop</strong>: IntraBay - 특정 제조 장비 구역으로 연결되는 분기 레일</li>
                </ul>
            </div>
            
            <div class="layout-section">
                <h5>⚡ 단축 경로 시스템</h5>
                <ul>
                    <li><strong>shortcut</strong>: 중앙 루프나 베이 간 이동 거리 단축을 위한 추가 트랙</li>
                    <li><strong>shortcut_central_loop</strong>: 중앙 루프에 위치하는 단축 포인트</li>
                    <li><strong>shortcut_layer_local_loop</strong>: 층간 이동을 위한 단축 포인트 (버퍼 공간 활용)</li>
                    <li><strong>shortcut_local_loop</strong>: 로컬 루프에 위치하는 단축 포인트</li>
                </ul>
            </div>
        </div>
        
        <div class="instruction-box">
            <p><strong>📝 사용 방법:</strong> 초기 값은 input.sample.json에서 로드되며, 수정 후 "update to layout_seed.input" 버튼을 클릭하여 데이터베이스에 저장합니다.</p>
        </div>
    `;
    container.appendChild(description);
    const form = document.createElement('form');
    form.id = 'inputForm';
    form.onsubmit = updateInputJson;

    function createFormFieldsDOM(obj, prefix = '') {
        const fragment = document.createDocumentFragment();
        for (const [key, value] of Object.entries(obj)) {
            const fieldName = prefix ? `${prefix}.${key}` : key;
            if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                const section = document.createElement('div');
                section.className = 'input-section';
                const sectionTitle = document.createElement('h4');
                sectionTitle.textContent = `🔧 ${key}`;
                section.appendChild(sectionTitle);
                const nestedFields = createFormFieldsDOM(value, fieldName);
                section.appendChild(nestedFields);
                fragment.appendChild(section);
            } else {
                const formGroup = document.createElement('div');
                formGroup.className = 'form-group';
                const label = document.createElement('label');
                label.textContent = `${key}:`;
                formGroup.appendChild(label);
                const textarea = document.createElement('textarea');
                textarea.name = fieldName;
                textarea.rows = Array.isArray(value) ? Math.min(value.length + 2, 10) : 4;
                textarea.className = 'form-control';
                textarea.value = JSON.stringify(value, null, 2);
                formGroup.appendChild(textarea);
                fragment.appendChild(formGroup);
            }
        }
        return fragment;
    }

    const formFields = createFormFieldsDOM(inputData);
    form.appendChild(formFields);
    const actionsDiv = document.createElement('div');
    actionsDiv.className = 'form-actions';
    const submitButton = document.createElement('button');
    submitButton.type = 'submit';
    submitButton.className = 'update-btn';
    submitButton.textContent = 'update to layout_seed.input';
    actionsDiv.appendChild(submitButton);
    form.appendChild(actionsDiv);
    container.appendChild(form);
    resultArea.innerHTML = '';
    resultArea.appendChild(container);
}

async function updateInputJson(event) {
    if (event) event.preventDefault();
    try {
        showLoading();
        const form = (event && event.target && (event.target.closest ? event.target.closest('form') : event.target))
            || document.getElementById('inputForm')
            || document.querySelector('#resultArea form')
            || document.querySelector('form');
        if (!form) throw new Error('inputForm을 찾을 수 없습니다.');
        const formData = new FormData(form);
        const inputData = {};
        for (let [key, value] of formData.entries()) {
            const keys = key.split('.');
            let current = inputData;
            for (let i = 0; i < keys.length - 1; i++) {
                if (!current[keys[i]]) current[keys[i]] = {};
                current = current[keys[i]];
            }
            current[keys[keys.length - 1]] = JSON.parse(value);
        }
        const response = await fetch('/api/update-input-json', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(inputData)
        });
        const result = await response.json();
        if (result.success) showStatus('✅ DB에 성공적으로 저장되었습니다.', 'success');
        else showStatus('❌ DB 저장 실패: ' + result.message, 'error');
    } catch (error) {
        showStatus(`DB 저장 중 오류가 발생했습니다: ${error.message}`, 'error');
    } finally { hideLoading(); }
}


