let sidebarVisible = true;

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');
    const toggleBtn = document.querySelector('.sidebar-toggle');
    sidebarVisible = !sidebarVisible;
    if (sidebarVisible) {
        sidebar.classList.remove('hidden');
        mainContent.classList.remove('expanded');
        toggleBtn.style.left = '200px';
    } else {
        sidebar.classList.add('hidden');
        mainContent.classList.add('expanded');
        toggleBtn.style.left = '20px';
    }
}

function showStatus(message, type = 'info') {
    const statusArea = document.getElementById('statusArea');
    statusArea.innerHTML = `<div class="status-message status-${type}">${message}</div>`;
}

function showLoading() {
    document.getElementById('loadingArea').style.display = 'block';
    document.getElementById('resultArea').innerHTML = '';
}

function hideLoading() {
    document.getElementById('loadingArea').style.display = 'none';
}

function hideMenuGuide() {
    const menuGuide = document.querySelector('.menu-guide');
    if (menuGuide) {
        menuGuide.style.display = 'none';
    }
}

function hideWelcomeMessage() {
    const welcomeMessage = document.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.style.display = 'none';
    }
}

function showMenuGuide() {
    const menuGuide = document.querySelector('.menu-guide');
    const welcomeMessage = document.querySelector('.welcome-message');
    if (menuGuide) {
        menuGuide.style.display = 'block';
    }
    if (welcomeMessage) {
        welcomeMessage.style.display = 'block';
    }
    // 결과 영역과 상태 영역 초기화
    document.getElementById('resultArea').innerHTML = '';
    document.getElementById('statusArea').innerHTML = '';
}

function executeSequentialDataGeneration() {
    // 메뉴 가이드와 환영 메시지 숨김
    hideMenuGuide();
    hideWelcomeMessage();
    
    // 로딩 표시
    showLoading();
    showStatus('🚀 Data AutoGen을 시작합니다...', 'info');
    
    // 순차적으로 실행할 메뉴들 (1-6단계)
    const sequentialMenus = [
        { id: 'layout_seed', name: '1. Layout Seed' },
        { id: 'add_addresses', name: '2. add Addresses' }, 
        { id: 'add_lines_endpoint', name: '3. add Lines' },
        { id: 'stations', name: '4. add Stations' },
        { id: 'check', name: '5. check Errors' },
        { id: 'udp_generator', name: '6. make OHTs Simulation' }
    ];
    
    let currentIndex = 0;
    
    function executeNext() {
        if (currentIndex >= sequentialMenus.length) {
            hideLoading();
            showStatus('🎉 모든 데이터 생성 단계가 완료되었습니다!', 'success');
            return;
        }
        
        const menu = sequentialMenus[currentIndex];
        
        showStatus(`실행 중: ${menu.name} (${currentIndex + 1}/${sequentialMenus.length})`, 'info');
        
        // 실제 메뉴 실행 로직 (비동기로 처리)
        executeActionAsync(menu.id)
            .then(() => {
                currentIndex++;
                // 1초 후 다음 단계로 진행 (완료 후 적절한 간격)
                setTimeout(executeNext, 1000);
            })
            .catch(error => {
                hideLoading();
                showStatus(`❌ ${menu.name} 실행 중 오류 발생: ${error.message}`, 'error');
            });
    }
    
    executeNext();
}

// 비동기 메뉴 실행 함수
async function executeActionAsync(menuType) {
    return new Promise((resolve, reject) => {
        // Layout Seed는 API 호출 없이 직접 처리
        if (menuType === 'layout_seed') {
            if (typeof runLayoutSeed === 'function') {
                runLayoutSeed();
                showStatus('Layout Seed 초기화 완료', 'success');
                resolve();
            } else {
                reject(new Error('Layout Seed 초기화 실패'));
            }
            return;
        }
        
        // 다른 메뉴들은 API 호출
        const apiEndpoints = {
            'add_addresses': '/api/run-generate',
            'add_lines_endpoint': '/api/run-add-lines',
            'stations': '/api/run-stations',
            'check': '/api/run-check',
            'udp_generator': '/api/run-udp-generator'
        };
        
        const endpoint = apiEndpoints[menuType];
        if (!endpoint) {
            reject(new Error(`알 수 없는 메뉴 타입: ${menuType}`));
            return;
        }
        
        const body = menuType === 'udp_generator' ? JSON.stringify(
            getOHTDefaultValues().map((addresses, idx) => ({
                startAddress: addresses[0],
                destinationAddress: addresses[1],
                ohtId: `OHT_${idx}`
            }))
        ) : undefined;
        
        fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: body
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showStatus(`✅ ${menuType} 완료: ${data.message}`, 'success');
                
                // UDP Generator 실행 후 자동으로 OHT 로그 업데이트
                if (menuType === 'udp_generator') {
                    updateOhtLogToFile();
                }
                
                resolve(data);
            } else {
                reject(new Error(`${menuType} 실패: ${data.message}`));
            }
        })
        .catch(error => {
            reject(error);
        });
    });
}

// 기존 동기 메뉴 실행 함수 (개별 메뉴 클릭용)
function executeAction(menuType) {
    const statusArea = document.getElementById('statusArea');
    const resultArea = document.getElementById('resultArea');
    
    statusArea.innerHTML = `<p>${menuType} 실행 중...</p>`;
    resultArea.innerHTML = '';
    

    // menuType을 실제 API 엔드포인트로 매핑
    const apiEndpoints = {
        'layout_seed': '/api/get-input-data',  // GET 요청
        'add_addresses': '/api/run-generate',
        'add_lines_endpoint': '/api/run-add-lines',
        'stations': '/api/run-stations',
        'check': '/api/run-check',
        'udp_generator': '/api/run-udp-generator'
    };
    
    const endpoint = apiEndpoints[menuType];
    if (!endpoint) {
        showStatus(`❌ 알 수 없는 메뉴 타입: ${menuType}`, 'error');
        return;
    }
    
    const method = menuType === 'layout_seed' ? 'GET' : 'POST';
    
    // UDP Generator의 경우 10개의 OHT 데이터를 모두 전송
    const body = menuType === 'udp_generator' ? JSON.stringify(
        getOHTDefaultValues().map((addresses, idx) => ({
            startAddress: addresses[0],
            destinationAddress: addresses[1],
            ohtId: `OHT_${idx}`
        }))
    ) : undefined;
    
    fetch(endpoint, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: body
    })
    .then(response => response.json())
    .then(data => {
        if (menuType === 'layout_seed') {
            // layout_seed는 GET 요청이므로 다른 응답 구조
            statusArea.innerHTML = `<p style="color: green;">${menuType} 완료: Layout Seed 데이터를 가져왔습니다.</p>`;
            resultArea.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
        } else if (data.success) {
            statusArea.innerHTML = `<p style="color: green;">${menuType} 완료: ${data.message}</p>`;
            resultArea.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
            
            // UDP Generator 실행 후 자동으로 OHT 로그 업데이트
            if (menuType === 'udp_generator') {
                console.log('UDP Generator completed, scheduling OHT log update...');
                setTimeout(() => {
                    console.log('Executing automatic OHT log update...');
                    updateOhtLogToFileWithRetry();
                }, 3000); // 3초 후 실행 (DB 트랜잭션 완료 대기)
            }
        } else {
            statusArea.innerHTML = `<p style="color: red;">${menuType} 실패: ${data.message}</p>`;
            resultArea.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
        }
        
        // 작업 완료 후 상태 정보 업데이트
        loadStatusInfo();
    })
    .catch(error => {
        statusArea.innerHTML = `<p style="color: red;">${menuType} 오류: ${error.message}</p>`;
        resultArea.innerHTML = `<pre>${error}</pre>`;
    });
}

// OHT 로그를 파일로 업데이트하는 함수 (재시도 로직 포함)
async function updateOhtLogToFileWithRetry(maxRetries = 3, delay = 2000) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            console.log(`Starting OHT log update (attempt ${attempt}/${maxRetries})...`);
            
            const response = await fetch('/api/update-oht-log', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });
            
            console.log('OHT log update response status:', response.status);
            const result = await response.json();
            console.log('OHT log update result:', result);
            
            if (result.success) {
                // 성공해도 메시지 출력하지 않음 (조용히 성공)
                console.log('OHT log update successful, but not showing message to user');
                return; // 성공 시 함수 종료
            } else {
                console.log(`Attempt ${attempt} failed: ${result.message}`);
                if (attempt < maxRetries) {
                    console.log(`Retrying in ${delay}ms...`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                } else {
                    // 실패해도 메시지 출력하지 않음 (조용히 실패)
                    console.log('OHT log update failed after all retries, but not showing error to user');
                }
            }
        } catch (error) {
            console.error(`OHT log update error (attempt ${attempt}):`, error);
            if (attempt < maxRetries) {
                console.log(`Retrying in ${delay}ms...`);
                await new Promise(resolve => setTimeout(resolve, delay));
            } else {
                // 에러가 발생해도 메시지 출력하지 않음 (조용히 실패)
                console.log('OHT log update error after all retries, but not showing error to user');
            }
        }
    }
}

// OHT 로그를 파일로 업데이트하는 함수 (기존 함수 유지)
async function updateOhtLogToFile() {
    try {
        console.log('Starting OHT log update...');
        
        const response = await fetch('/api/update-oht-log', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        console.log('OHT log update response status:', response.status);
        const result = await response.json();
        console.log('OHT log update result:', result);
        
        if (result.success) {
            showStatus('✅ OHT 로그가 oht_track_data.log 파일로 업데이트되었습니다.', 'success');
        } else {
            // 실패해도 메시지 출력하지 않음 (조용히 실패)
            console.log('OHT log update failed, but not showing error to user');
        }
    } catch (error) {
        console.error('OHT log update error:', error);
        // 에러가 발생해도 메시지 출력하지 않음 (조용히 실패)
        console.log('OHT log update error, but not showing error to user');
    }
}

// 상태 정보 로딩
function loadStatusInfo() {
    fetch('/api/status', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // 사용자 정보 업데이트
            document.getElementById('user-id').textContent = data.userInfo.userId;
            document.getElementById('last-activity').textContent = data.userInfo.lastActivity;
            
            // 데이터베이스 상태 업데이트
            document.getElementById('table-name').textContent = data.dbStatus.tableName;
            document.getElementById('db-connection').textContent = data.dbStatus.connection;
        } else {
            console.error('상태 정보 로딩 실패:', data.message);
        }
    })
    .catch(error => {
        console.error('상태 정보 로딩 중 오류:', error);
    });
}

// Top 메뉴 클릭 시 메뉴 가이드 숨김 처리
document.addEventListener('DOMContentLoaded', function() {
    // 페이지 로드 시 상태 정보 로딩
    loadStatusInfo();
    
    // 5초마다 상태 정보 업데이트
    setInterval(loadStatusInfo, 5000);
    
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            const viewType = this.getAttribute('data-view');
            
            // Data AutoGen 클릭 시 순차 실행
            if (viewType === 'data_autogen') {
                executeSequentialDataGeneration();
            } else {
                hideMenuGuide();
                hideWelcomeMessage();
            }
        });
    });
    
    // 로고 클릭 시 메뉴 가이드 표시
    const logo = document.querySelector('.logo');
    if (logo) {
        logo.addEventListener('click', function(e) {
            e.preventDefault();
            showMenuGuide();
        });
    }
});




