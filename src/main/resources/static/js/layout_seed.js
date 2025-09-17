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

// 고정 버튼 정리 함수 (하위 호환성을 위해 유지)
function cleanupFixedButton() {
    const existingFixedButton = document.getElementById('fixed-update-button');
    if (existingFixedButton) {
        existingFixedButton.remove();
    }
}

// 샘플 선택기 생성 함수
function createSampleSelector() {
    const selectorContainer = document.createElement('div');
    selectorContainer.className = 'sample-selector-container';
    
    const selectorTitle = document.createElement('h4');
    selectorTitle.textContent = '📂 샘플 데이터 선택';
    selectorTitle.className = 'sample-selector-title';
    selectorContainer.appendChild(selectorTitle);
    
    const selectorGrid = document.createElement('div');
    selectorGrid.className = 'sample-selector-grid';
    
            // 샘플 데이터 정보
            const samples = [
                {
                    number: 1,
                    name: 'Complex Layout',
                    description: '3층 구조 레이아웃',
                    image: '/images/input_sample1.png'
                },
                {
                    number: 2,
                    name: 'Extended Layout', 
                    description: '2층 구조 레이아웃',
                    image: '/images/input_sample2.png'
                },
                {
                    number: 3,
                    name: 'Basic Layout',
                    description: '1층 구조 레이아웃',
                    image: '/images/input_sample3.png'
                }
            ];
    
        samples.forEach(sample => {
            const sampleCard = document.createElement('div');
            sampleCard.className = 'sample-card';
            sampleCard.setAttribute('data-sample', sample.number);
            sampleCard.innerHTML = `
                <div class="sample-image-container">
                    <img src="${sample.image}" alt="${sample.name}" class="sample-image" />
                    <div class="sample-overlay">
                        <div class="sample-load-icon">📥</div>
                        <div class="sample-load-text">Load</div>
                    </div>
                </div>
                <div class="sample-info">
                    <h5 class="sample-title">Sample ${sample.number}</h5>
                    <p class="sample-name">${sample.name}</p>
                    <p class="sample-description">${sample.description}</p>
                </div>
            `;
            
            // 카드 클릭 이벤트 추가 (전체 카드가 클릭 가능)
            sampleCard.addEventListener('click', () => loadSampleData(sample.number));
            
            selectorGrid.appendChild(sampleCard);
        });
    
    selectorContainer.appendChild(selectorGrid);
    return selectorContainer;
}

// 샘플 데이터 로드 함수 (최적화된 버전)
async function loadSampleData(sampleNumber) {
    // 입력 검증
    if (!sampleNumber || sampleNumber < 1 || sampleNumber > 3) {
        showStatus('유효하지 않은 샘플 번호입니다.', 'error');
        return;
    }

    try {
        showLoading();
        console.log(`🔄 샘플 ${sampleNumber} 로드 시작...`);
        
        // AbortController를 사용한 요청 타임아웃 처리
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10초 타임아웃
        
        const response = await fetch(`/api/get-sample-data/${sampleNumber}`, {
            signal: controller.signal,
            headers: {
                'Content-Type': 'application/json',
                'Cache-Control': 'no-cache'
            }
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const result = await response.json();
        
        if (result.success && result.data) {
            console.log(`✅ 샘플 ${sampleNumber} 로드 성공`);
            showStatus(`샘플 ${sampleNumber} 데이터를 로드 중...`, 'info');
            
            // 1. 기존 폼을 새 데이터로 교체
            replaceFormWithNewData(result.data);
            
            // 2. DB에 저장
            await saveSampleToDatabase(result.data, sampleNumber);
        } else {
            throw new Error(result.message || '샘플 데이터를 로드할 수 없습니다.');
        }
    } catch (error) {
        console.error(`💥 샘플 ${sampleNumber} 로드 중 오류:`, error);
        
        if (error.name === 'AbortError') {
            showStatus(`샘플 ${sampleNumber} 로드 시간이 초과되었습니다.`, 'error');
        } else {
            showStatus(`샘플 ${sampleNumber} 로드 중 오류가 발생했습니다: ${error.message}`, 'error');
        }
    } finally {
        hideLoading();
    }
}

// 폼을 새 데이터로 교체하는 함수
function replaceFormWithNewData(newData) {
    console.log('🔄 폼 데이터 교체 시작...');
    
    // 기존 폼 찾기
    const existingForm = document.getElementById('inputForm');
    if (!existingForm) {
        console.error('❌ 기존 폼을 찾을 수 없습니다!');
        return;
    }
    
    // 새 폼 필드 생성
    const newFormFields = createFormFieldsDOM(newData);
    
    // 기존 폼 내용을 모두 지우고 새 필드로 교체
    existingForm.innerHTML = '';
    existingForm.appendChild(newFormFields);
    
    console.log('✅ 폼 데이터 교체 완료!');
    console.log('📊 새 데이터 구조:', Object.keys(newData));
}

// 샘플 데이터를 DB에 저장하는 함수
async function saveSampleToDatabase(sampleData, sampleNumber) {
    try {
        console.log(`💾 샘플 ${sampleNumber} DB 저장 시작...`);
        
        const response = await fetch('/api/update-input-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sampleData)
        });
        
        const result = await response.json();
        
        if (result.success) {
            console.log(`✅ 샘플 ${sampleNumber} DB 저장 성공`);
            showStatus(`✅ 샘플 ${sampleNumber} 데이터가 DB에 저장되었습니다. Layout Seed로 복귀합니다...`, 'success');
            
            // 1초 후 Layout Seed로 자동 복귀
            setTimeout(() => {
                console.log('🔄 샘플 로드 완료 후 Layout Seed 자동 복귀...');
                runLayoutSeed();
            }, 1000);
        } else {
            console.error(`❌ 샘플 ${sampleNumber} DB 저장 실패:`, result.message);
            showStatus(`❌ 샘플 ${sampleNumber} 에디터 로드는 성공했지만 DB 저장 실패: ${result.message}`, 'error');
        }
    } catch (error) {
        console.error(`💥 샘플 ${sampleNumber} DB 저장 중 오류:`, error);
        showStatus(`샘플 ${sampleNumber} DB 저장 중 오류가 발생했습니다: ${error.message}`, 'error');
    }
}

function displayInputForm(inputData) {
    const resultArea = document.getElementById('resultArea');
    const container = document.createElement('div');
    container.className = 'execution-result';
    const title = document.createElement('h3');
    title.textContent = '🔧 Layout Seed - 샘플 기반 편집';
    container.appendChild(title);
    
    // 샘플 선택 UI 추가
    const sampleSelector = createSampleSelector();
    container.appendChild(sampleSelector);
    
    const description = document.createElement('div');
    description.className = 'layout-description';
    description.innerHTML = `
        <div class="instruction-box">
            <p><strong>📝 사용 방법:</strong> 위에서 원하는 샘플을 선택하거나, 현재 데이터를 편집한 후 "update to layout_seed.input" 버튼을 클릭하여 데이터베이스에 저장합니다.</p>
        </div>
    `;
    container.appendChild(description);
    const form = document.createElement('form');
    form.id = 'inputForm';
    form.onsubmit = updateInputJson;

    function createFormFieldsDOM(obj, prefix = '', depth = 0) {
        const fragment = document.createDocumentFragment();
        let fieldCount = 0;
        
        console.log(`🏗️ createFormFieldsDOM 호출 - depth: ${depth}, prefix: "${prefix}"`);
        console.log(`📋 처리할 객체 키들:`, Object.keys(obj));
        
        for (const [key, value] of Object.entries(obj)) {
            const fieldName = prefix ? `${prefix}.${key}` : key;
            
            if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                console.log(`📁 섹션 생성: ${fieldName} (중첩 객체)`);
                const section = document.createElement('div');
                section.className = 'input-section';
                const sectionTitle = document.createElement('h4');
                sectionTitle.textContent = `🔧 ${key}`;
                section.appendChild(sectionTitle);
                const nestedFields = createFormFieldsDOM(value, fieldName, depth + 1);
                section.appendChild(nestedFields);
                fragment.appendChild(section);
            } else {
                console.log(`📝 필드 생성: ${fieldName}`, {
                    type: typeof value,
                    isArray: Array.isArray(value),
                    valueLength: Array.isArray(value) ? value.length : (typeof value === 'string' ? value.length : 'N/A')
                });
                
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
                fieldCount++;
            }
        }
        
        console.log(`✅ createFormFieldsDOM 완료 - depth: ${depth}, 생성된 필드 수: ${fieldCount}`);
        return fragment;
    }

    console.log('🚀 폼 필드 생성 시작...');
    console.log('📊 inputData 구조:', Object.keys(inputData));
    const formFields = createFormFieldsDOM(inputData);
    form.appendChild(formFields);
    console.log('✅ 폼 필드 생성 완료!');
    
    // 기존 고정 버튼들 제거
    if (typeof cleanupAllFixedButtons === 'function') {
        cleanupAllFixedButtons();
    }
    
    // 화면 하단에 고정될 버튼 생성
    const fixedButtonContainer = document.createElement('div');
    fixedButtonContainer.id = 'fixed-update-button';
    fixedButtonContainer.className = 'fixed-button-container';
    
    const submitButton = document.createElement('button');
    submitButton.type = 'button'; // submit에서 button으로 변경
    submitButton.className = 'fixed-update-btn';
    submitButton.textContent = '💾 update to layout_seed.input';
    submitButton.onclick = (e) => {
        e.preventDefault();
        // 명시적으로 폼을 찾아서 전달
        const targetForm = document.getElementById('inputForm');
        if (targetForm) {
            console.log('🎯 고정 버튼에서 폼 찾기 성공:', targetForm);
            updateInputJson({ target: targetForm, preventDefault: () => {} });
        } else {
            console.error('❌ inputForm을 찾을 수 없습니다!');
            alert('폼을 찾을 수 없습니다. 페이지를 새로고침 후 다시 시도해주세요.');
        }
    };
    
    fixedButtonContainer.appendChild(submitButton);
    document.body.appendChild(fixedButtonContainer);
    
    container.appendChild(form);
    resultArea.innerHTML = '';
    resultArea.appendChild(container);
}

async function updateInputJson(event) {
    if (event) event.preventDefault();
    try {
        showLoading();
        
        // 폼 찾기 (간소화된 로직)
        const form = findInputForm();
        if (!form) {
            throw new Error('입력 폼을 찾을 수 없습니다. 페이지를 새로고침 후 다시 시도해주세요.');
        }
        
        // 🔍 2. FormData 수집 디버깅
        const formData = new FormData(form);
        const formEntries = [...formData.entries()];
        console.log('📝 FormData 항목 수:', formEntries.length);
        
        // ⚠️ FormData가 비어있는지 확인
        if (formEntries.length === 0) {
            console.warn('⚠️ FormData가 비어있습니다!');
            console.log('🔍 폼 내부 요소 확인:');
            console.log('  - 폼 ID:', form.id);
            console.log('  - 폼 내 input 수:', form.querySelectorAll('input, textarea, select').length);
            console.log('  - 폼 내 textarea 수:', form.querySelectorAll('textarea').length);
            
            // 폼 내부 textarea들 확인
            const textareas = form.querySelectorAll('textarea');
            textareas.forEach((textarea, idx) => {
                console.log(`    textarea ${idx + 1}: name="${textarea.name}", value length=${textarea.value.length}`);
            });
        }
        
        // 🔍 3. 폼 데이터 상세 분석
        console.log('📋 수집된 폼 데이터:');
        formEntries.forEach(([key, value], index) => {
            console.log(`  ${index + 1}. ${key}:`, value.substring(0, 100) + (value.length > 100 ? '...' : ''));
        });
        
        // 🔍 4. JSON 변환 과정 디버깅
        const inputData = {};
        let successCount = 0;
        let errorCount = 0;
        
        for (let [key, value] of formData.entries()) {
            try {
                const keys = key.split('.');
                let current = inputData;
                for (let i = 0; i < keys.length - 1; i++) {
                    if (!current[keys[i]]) current[keys[i]] = {};
                    current = current[keys[i]];
                }
                current[keys[keys.length - 1]] = JSON.parse(value);
                successCount++;
            } catch (parseError) {
                console.error(`❌ JSON 파싱 실패 - ${key}:`, parseError.message);
                console.error(`   원본 값:`, value);
                errorCount++;
            }
        }
        
        console.log(`✅ JSON 변환 완료: 성공 ${successCount}개, 실패 ${errorCount}개`);
        console.log('🏗️ 최종 inputData 구조:', Object.keys(inputData));
        console.log('📊 inputData 크기:', JSON.stringify(inputData).length, '바이트');
        
        // 🔍 5. API 요청 디버깅
        console.log('🚀 API 요청 시작...');
        const response = await fetch('/api/update-input-json', {
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(inputData)
        });
        
        console.log('📡 API 응답 상태:', response.status, response.statusText);
        const result = await response.json();
        console.log('📋 API 응답 결과:', result);
        
        if (result.success) {
            showStatus('✅ DB에 성공적으로 저장되었습니다.', 'success');
            console.log('✅ 데이터 저장 성공!');
        } else {
            showStatus('❌ DB 저장 실패: ' + result.message, 'error');
            console.error('❌ 데이터 저장 실패:', result.message);
        }
    } catch (error) {
        console.error('💥 updateInputJson 전체 오류:', error);
        showStatus(`DB 저장 중 오류가 발생했습니다: ${error.message}`, 'error');
    } finally { 
        hideLoading(); 
    }
}

// 유틸리티 함수들
function findInputForm() {
    const strategies = [
        () => document.getElementById('inputForm'),
        () => document.querySelector('#resultArea form#inputForm'),
        () => document.querySelector('#resultArea form'),
        () => document.querySelector('form[data-form-type="input"]'),
        () => document.querySelector('form')
    ];
    
    for (const strategy of strategies) {
        const form = strategy();
        if (form) return form;
    }
    
    return null;
}

function collectAndValidateFormData(form) {
    const formData = new FormData(form);
    const inputData = {};
    let processedCount = 0;
    
    for (const [key, value] of formData.entries()) {
        try {
            const keys = key.split('.');
            let current = inputData;
            
            for (let i = 0; i < keys.length - 1; i++) {
                if (!current[keys[i]]) current[keys[i]] = {};
                current = current[keys[i]];
            }
            
            current[keys[keys.length - 1]] = JSON.parse(value);
            processedCount++;
            
        } catch (parseError) {
            console.warn(`JSON 파싱 실패 - ${key}:`, parseError.message);
            const keys = key.split('.');
            let current = inputData;
            for (let i = 0; i < keys.length - 1; i++) {
                if (!current[keys[i]]) current[keys[i]] = {};
                current = current[keys[i]];
            }
            current[keys[keys.length - 1]] = value;
        }
    }
    
    console.log(`📊 폼 데이터 처리 완료: ${processedCount}개 필드`);
    return inputData;
}

async function submitDataToServer(inputData) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 15000);
    
    try {
        const response = await fetch('/api/update-input-json', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Cache-Control': 'no-cache'
            },
            body: JSON.stringify(inputData),
            signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return await response.json();
        
    } catch (error) {
        clearTimeout(timeoutId);
        if (error.name === 'AbortError') {
            throw new Error('요청 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.');
        }
        throw error;
    }
}
