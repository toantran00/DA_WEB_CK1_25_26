// Global variables
let currentUser = null;
let isEditMode = false;

// Load user profile when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadUserProfile();
    initProfileEventHandlers();
});

// Initialize event handlers
function initProfileEventHandlers() {
    // Tab navigation
    document.addEventListener('click', function(e) {
        if (e.target.closest('.nav-item')) {
            const navItem = e.target.closest('.nav-item');
            const tabName = navItem.getAttribute('data-tab');
            switchTab(tabName);
        }
    });

    // Password toggle
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('toggle-password')) {
            togglePasswordVisibility(e.target);
        }
    });

    // Avatar upload
    document.addEventListener('change', function(e) {
        if (e.target.id === 'avatarInput') {
            handleAvatarUpload(e.target);
        }
    });

    // Form submissions
    document.addEventListener('submit', function(e) {
        if (e.target.id === 'profileForm') {
            e.preventDefault();
            saveProfile();
        } else if (e.target.id === 'passwordForm') {
            e.preventDefault();
            changePassword();
        }
    });

    // Real-time validation for phone number
    document.addEventListener('input', function(e) {
        if (e.target.id === 'sdt' && isEditMode) {
            validatePhoneNumber(e.target);
        }
    });
}

// Validate phone number in real-time
function validatePhoneNumber(input) {
    const value = input.value.trim();
    const errorDiv = document.getElementById('sdtError');
    
    if (value === '') {
        // Clear error if empty (optional field)
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
    
    const phoneRegex = /^0[0-9]{9}$/;
    if (!phoneRegex.test(value)) {
        input.classList.add('error');
        errorDiv.textContent = 'Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số';
        errorDiv.style.display = 'block';
        return false;
    } else {
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
}

// Load user profile from API
async function loadUserProfile() {
    console.log('🔄 Bắt đầu load user profile...');
    
    // Show loading state
    showLoadingState();

    // Timeout để tránh loading vô hạn
    const timeoutId = setTimeout(() => {
        console.error('⏱️ Request timeout sau 10 giây');
        showError('Kết nối quá lâu. Vui lòng kiểm tra server có đang chạy không và thử lại.');
    }, 10000);

    try {
        console.log('📡 Đang gọi API /profile/api/user/profile...');
        
        const controller = new AbortController();
        const timeoutSignal = setTimeout(() => controller.abort(), 10000);
        
        // Lấy token từ localStorage (nếu có)
        const token = localStorage.getItem('jwtToken');
        console.log('Token from localStorage:', token ? 'Có' : 'Không có');
        
        // Gửi request - token sẽ được lấy từ cookie hoặc Authorization header
        const headers = {
            'Content-Type': 'application/json'
        };
        
        // Chỉ thêm Authorization header nếu có token trong localStorage
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }
        
        const response = await fetch('/profile/api/user/profile', {
            signal: controller.signal,
            headers: headers,
            credentials: 'include' // Quan trọng: gửi cookie cùng với request
        });

        clearTimeout(timeoutSignal);
        clearTimeout(timeoutId);

        console.log('📥 Response status:', response.status);

        if (response.status === 401) {
            console.error('❌ Token hết hạn (401)');
            localStorage.removeItem('jwtToken');
            localStorage.removeItem('user');
            showError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);
            return;
        }

        if (!response.ok) {
            console.error('❌ HTTP error:', response.status);
            const errorText = await response.text();
            console.error('Error response:', errorText);
            throw new Error(`HTTP error! status: ${response.status} - ${errorText}`);
        }

        const result = await response.json();
        console.log('📦 API Response:', result);

        if (result.success) {
            console.log('✅ Load profile thành công');
            currentUser = result.data;
            displayUserProfile(currentUser);
            showProfileContent();
        } else {
            console.error('❌ API trả về lỗi:', result.message);
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        clearTimeout(timeoutId);
        console.error('❌ Profile loading error:', error);
        console.error('Error details:', error.message, error.stack);
        
        if (error.name === 'AbortError') {
            showError('Kết nối quá lâu (timeout). Vui lòng kiểm tra server và thử lại.');
        } else {
            showError('Lỗi kết nối API. Kiểm tra server có đang chạy không. Chi tiết: ' + error.message);
        }
    }
}

// Display user profile
function displayUserProfile(user) {
    if (!user) {
        showError('Không có dữ liệu người dùng');
        return;
    }

    try {
        // Safely handle user data with proper escaping
        const tenNguoiDung = escapeHtml(user.tenNguoiDung || 'Chưa có tên');
        const email = escapeHtml(user.email || '');
        const sdt = escapeHtml(user.sdt || 'Chưa cập nhật');
        const diaChi = escapeHtml(user.diaChi || '');
        const trangThai = escapeHtml(user.trangThai || 'Hoạt động');
        const vaiTroTen = escapeHtml(user.vaiTro?.tenVaiTro || 'USER');

        // Determine status badge color
        let statusColor = '#6c757d'; // default
        if (user.trangThai === 'Hoạt động') {
            statusColor = '#28a745';
        } else if (user.trangThai === 'Tạm khóa') {
            statusColor = '#ffc107';
        } else if (user.trangThai === 'Khóa') {
            statusColor = '#dc3545';
        }
        
        // Determine status badge background color
        let statusBg = '#e9ecef';
        if (user.trangThai === 'Hoạt động') {
            statusBg = '#d4edda'; // Light green
        } else if (user.trangThai === 'Tạm khóa') {
            statusBg = '#fff3cd'; // Light yellow
        } else if (user.trangThai === 'Khóa') {
            statusBg = '#f8d7da'; // Light red
        }


        // Construct avatar image path - Fix path to use /uploads/users/ instead of /uploads/images/
        const avatarPath = user.hinhAnh ? `/uploads/users/${user.hinhAnh}` : '/images/default-avatar.jpg';

        const profileHTML = `
            <div class="profile-content">
                <div class="profile-sidebar">
                    <div class="user-avatar">
                        <img src="${avatarPath}" 
                             alt="Avatar" class="avatar-image" id="avatarPreview">
                        <div class="avatar-upload">
                            <label for="avatarInput" class="avatar-upload-label">
                                <i class="fas fa-camera"></i> Đổi ảnh
                            </label>
                            <input type="file" id="avatarInput" accept="image/*" style="display: none;">
                        </div>
                    </div>
                    
                    <div class="user-info-sidebar">
                        <h3>${tenNguoiDung}</h3>
                        <p><i class="fas fa-envelope"></i> ${email}</p>
                        <p><i class="fas fa-phone"></i> ${sdt}</p>
                        
                        <div class="role-status-container">
                            <div class="user-role">${vaiTroTen}</div>
                            <div class="user-status" style="background-color: ${statusBg}; color: ${statusColor};">
                                <i class="fas fa-circle" style="color: ${statusColor};"></i> ${trangThai}
                            </div>
                        </div>
                    </div>

                    <div class="profile-nav">
                        <div class="nav-item active" data-tab="profile">
                            <i class="fas fa-user"></i>
                            <span>Thông tin cá nhân</span>
                        </div>
                        <div class="nav-item" data-tab="addresses">
                            <i class="fas fa-map-marker-alt"></i>
                            <span>Địa chỉ nhận hàng</span>
                        </div>
                        <div class="nav-item" data-tab="security">
                            <i class="fas fa-shield-alt"></i>
                            <span>Đổi mật khẩu</span>
                        </div>
                    </div>
                </div>

                <div class="profile-main">
                    <div class="profile-tab active" id="profileTab">
                        <h2 class="section-title">Thông tin cá nhân</h2>
                        <form id="profileForm">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label class="form-label">Họ và tên *</label>
                                    <input type="text" class="form-input" id="tenNguoiDung" name="tenNguoiDung" 
                                           value="${tenNguoiDung}" disabled required>
                                    <div class="form-error" id="tenNguoiDungError">Vui lòng nhập họ và tên</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Email *</label>
                                    <input type="email" class="form-input" value="${email}" readonly>
                                    <small style="color: #666; font-size: 12px;">Email không thể thay đổi</small>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Số điện thoại</label>
                                    <input type="tel" class="form-input" id="sdt" name="sdt" 
                                           value="${sdt === 'Chưa cập nhật' ? '' : sdt}" 
                                           pattern="^0[0-9]{9}$"
                                           placeholder="Ví dụ: 0123456789" disabled>
                                    <div class="form-error" id="sdtError">Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Vai trò</label>
                                    <input type="text" class="form-input" 
                                           value="${vaiTroTen}" readonly>
                                    <small style="color: #666; font-size: 12px;">Vai trò được quản lý bởi hệ thống</small>
                                </div>

                                <div class="form-group full-width">
                                    <label class="form-label">Địa chỉ</label>
                                    <div class="address-input-group">
                                        <textarea class="form-input" id="diaChi" name="diaChi" rows="2" 
                                                  placeholder="Nhập địa chỉ của bạn hoặc chọn trên bản đồ" disabled>${diaChi}</textarea>
                                    </div>
                                    <input type="hidden" id="profileLatitude" value="">
                                    <input type="hidden" id="profileLongitude" value="">
                                    
                                    <div id="profileMapControls" style="display: none; margin-top: 10px;">
                                        <div class="map-controls">
                                            <button type="button" class="btn btn-sm secondary" onclick="searchProfileAddressOnMap()">
                                                <i class="fas fa-search"></i> Tìm trên bản đồ
                                            </button>
                                            <button type="button" class="btn btn-sm secondary" onclick="getProfileCurrentLocation()">
                                                <i class="fas fa-location-arrow"></i> Vị trí hiện tại
                                            </button>
                                        </div>
                                        
                                        <div class="map-container" style="height: 300px; margin-top: 10px;">
                                            <div id="profileMap"></div>
                                        </div>
                                        
                                        <div class="map-info">
                                            <i class="fas fa-info-circle"></i>
                                            <span>Bạn có thể kéo marker hoặc click vào bản đồ để chọn vị trí chính xác</span>
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group full-width">
                                    <label class="form-label">Trạng thái tài khoản</label>
                                    <input type="text" class="form-input" 
                                           value="${trangThai}" readonly>
                                    <small style="color: #666; font-size: 12px;">Trạng thái được quản lý bởi hệ thống</small>
                                </div>
                            </div>
                            
                            <div class="form-actions">
                                <button type="button" class="btn btn-primary" id="editProfileBtn" onclick="toggleEditMode()">
                                    <i class="fas fa-edit"></i> Cập nhật thông tin
                                </button>
                                <button type="submit" class="btn btn-success" id="saveProfileBtn" style="display: none;">
                                    <i class="fas fa-save"></i> Lưu
                                </button>
                                <button type="button" class="btn btn-secondary" id="cancelProfileBtn" style="display: none;" onclick="cancelEdit()">
                                    <i class="fas fa-times"></i> Hủy
                                </button>
                            </div>
                        </form>
                    </div>

                    <div class="profile-tab" id="addressesTab">
                        <h2 class="section-title">Địa chỉ nhận hàng</h2>
                        <div id="addressesContainer">
                            <div class="loading-spinner">Đang tải...</div>
                        </div>
                    </div>

                    <div class="profile-tab" id="securityTab">
                        <h2 class="section-title">Đổi mật khẩu</h2>
                        <form id="passwordForm">
                            <div class="form-grid">
                                <div class="form-group full-width">
                                    <label class="form-label">Mật khẩu hiện tại *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="currentPassword" name="currentPassword" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="currentPasswordError">Vui lòng nhập mật khẩu hiện tại</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Mật khẩu mới *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="newPassword" name="newPassword" 
                                               minlength="6" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="newPasswordError">Mật khẩu phải có ít nhất 6 ký tự</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Xác nhận mật khẩu *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="confirmPassword" name="confirmPassword" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="confirmPasswordError">Mật khẩu xác nhận không khớp</div>
                                </div>
                            </div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-key"></i> Đổi mật khẩu
                                </button>
                                <button type="button" class="btn btn-secondary" onclick="resetPasswordForm()">
                                    <i class="fas fa-undo"></i> Đặt lại
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            
            <!-- Modal thêm/sửa địa chỉ -->
            <div id="addressModal" class="modal" style="display: none;">
                <div class="modal-content modal-with-map">
                    <div class="modal-header">
                        <h3 id="addressModalTitle">Thêm địa chỉ mới</h3>
                        <button class="modal-close" onclick="closeAddressModal()">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                    <form id="addressForm" onsubmit="event.preventDefault(); saveAddress();">
                        <input type="hidden" id="addressId">
                        <input type="hidden" id="latitude">
                        <input type="hidden" id="longitude">
                        <div class="form-group">
                            <label class="form-label">Tên người nhận *</label>
                            <input type="text" class="form-input" id="tenNguoiNhan" required maxlength="100">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Số điện thoại *</label>
                            <input type="tel" class="form-input" id="soDienThoaiAddress" 
                                   pattern="^0[0-9]{9}$" required placeholder="0123456789">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Địa chỉ chi tiết *</label>
                            <div class="address-input-group">
                                <textarea class="form-input" id="diaChiChiTiet" rows="3" required maxlength="500" 
                                          placeholder="Nhập địa chỉ hoặc chọn trên bản đồ"></textarea>
                            </div>
                        </div>
                        
                        <div class="map-controls">
                            <button type="button" class="btn btn-sm secondary" onclick="searchAddressOnMap()">
                                <i class="fas fa-search"></i> Tìm trên bản đồ
                            </button>
                            <button type="button" class="btn btn-sm secondary" onclick="getCurrentLocationForAddress()">
                                <i class="fas fa-location-arrow"></i> Vị trí hiện tại
                            </button>
                        </div>
                        
                        <div class="map-container">
                            <div id="map"></div>
                        </div>
                        
                        <div class="map-info">
                            <i class="fas fa-info-circle"></i>
                            <span>Bạn có thể kéo marker hoặc click vào bản đồ để chọn vị trí chính xác</span>
                        </div>
                        
                        <div class="modal-actions">
                            <button type="submit" class="btn btn-primary">
                                <i class="fas fa-save"></i> Lưu
                            </button>
                            <button type="button" class="btn btn-secondary" onclick="closeAddressModal()">
                                <i class="fas fa-times"></i> Hủy
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;

        const profileContainer = document.getElementById('profileContent');
        if (profileContainer) {
            profileContainer.innerHTML = profileHTML;
        }

    } catch (error) {
        console.error('Error displaying profile:', error);
        showError('Lỗi hiển thị thông tin profile: ' + error.message);
    }
}

// ============= UI STATE MANAGEMENT FUNCTIONS =============

// Show loading state
function showLoadingState() {
    console.log('📊 Showing loading state...');
    const loadingState = document.getElementById('loadingState');
    const errorState = document.getElementById('errorState');
    const profileContent = document.getElementById('profileContent');
    
    if (loadingState) {
        loadingState.style.display = 'block';
    }
    if (errorState) {
        errorState.style.display = 'none';
    }
    if (profileContent) {
        profileContent.style.display = 'none';
    }
}

// Show error state
function showError(message) {
    console.log('❌ Showing error:', message);
    const loadingState = document.getElementById('loadingState');
    const errorState = document.getElementById('errorState');
    const profileContent = document.getElementById('profileContent');
    const errorMessage = document.getElementById('errorMessage');
    
    if (loadingState) {
        loadingState.style.display = 'none';
    }
    if (errorState) {
        errorState.style.display = 'block';
    }
    if (profileContent) {
        profileContent.style.display = 'none';
    }
    if (errorMessage) {
        errorMessage.textContent = message;
    }
}

// Show profile content
function showProfileContent() {
    console.log('✅ Showing profile content...');
    const loadingState = document.getElementById('loadingState');
    const errorState = document.getElementById('errorState');
    const profileContent = document.getElementById('profileContent');
    
    if (loadingState) {
        loadingState.style.display = 'none';
    }
    if (errorState) {
        errorState.style.display = 'none';
    }
    if (profileContent) {
        profileContent.style.display = 'block';
    }
}

// HTML escape function to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}

// Switch between tabs
function switchTab(tabName) {
    // Remove active class from all nav items and tabs
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelectorAll('.profile-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Add active class to selected nav item and tab
    const selectedNavItem = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
    const selectedTab = document.getElementById(`${tabName}Tab`);
    
    if (selectedNavItem) selectedNavItem.classList.add('active');
    if (selectedTab) selectedTab.classList.add('active');
    
    // Load addresses when switching to addresses tab
    if (tabName === 'addresses') {
        loadAddresses();
    }
}

// Toggle edit mode
function toggleEditMode() {
    isEditMode = !isEditMode;
    
    const editableFields = ['tenNguoiDung', 'sdt', 'diaChi'];
    const editBtn = document.getElementById('editProfileBtn');
    const saveBtn = document.getElementById('saveProfileBtn');
    const cancelBtn = document.getElementById('cancelProfileBtn');
    const mapControls = document.getElementById('profileMapControls');
    
    editableFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.disabled = !isEditMode;
        }
    });
    
    if (isEditMode) {
        if (editBtn) editBtn.style.display = 'none';
        if (saveBtn) saveBtn.style.display = 'inline-block';
        if (cancelBtn) cancelBtn.style.display = 'inline-block';
        if (mapControls) mapControls.style.display = 'block';
        
        // Focus on first editable field
        const firstField = document.getElementById('tenNguoiDung');
        if (firstField) {
            setTimeout(() => firstField.focus(), 100);
        }
        
        // Khởi tạo bản đồ cho profile
        setTimeout(() => {
            console.log('🗺️ Bắt đầu khởi tạo bản đồ cho profile...');
            
            const mapContainer = document.getElementById('profileMap');
            if (!mapContainer) {
                console.error('❌ Không tìm thấy profileMap container');
                return;
            }
            
            console.log('📦 Container profileMap found, dimensions:', 
                mapContainer.offsetWidth, 'x', mapContainer.offsetHeight);
            
            if (typeof L === 'undefined') {
                console.error('❌ Leaflet chưa được load!');
                alert('Thư viện bản đồ chưa sẵn sàng. Vui lòng refresh trang.');
                return;
            }
            
            if (window.mapsHelper) {
                // Lấy tọa độ từ user nếu có, nếu không dùng tọa độ mặc định (HCM)
                const lat = currentUser?.latitude || 10.762622;
                const lng = currentUser?.longitude || 106.660172;
                
                console.log('📍 Khởi tạo bản đồ tại tọa độ:', lat, lng);
                const mapInstance = window.mapsHelper.initMap('profileMap', lat, lng);
                
                if (mapInstance) {
                    console.log('✅ Bản đồ profile đã được khởi tạo thành công');
                    
                    // Force invalidate size nhiều lần để đảm bảo bản đồ hiển thị
                    setTimeout(() => {
                        if (mapInstance) {
                            mapInstance.invalidateSize();
                            console.log('🔄 Map size invalidated (1st time)');
                        }
                    }, 200);
                    
                    setTimeout(() => {
                        if (mapInstance) {
                            mapInstance.invalidateSize();
                            console.log('🔄 Map size invalidated (2nd time)');
                        }
                    }, 500);
                    
                    // Thiết lập callback để cập nhật tọa độ vào hidden fields
                    window.updateProfileCoordinates = function(lat, lng) {
                        const latField = document.getElementById('profileLatitude');
                        const lngField = document.getElementById('profileLongitude');
                        
                        if (latField) latField.value = lat;
                        if (lngField) lngField.value = lng;
                        
                        console.log('📍 Tọa độ profile đã cập nhật:', lat, lng);
                        
                        // Luôn enable textarea trước khi cập nhật địa chỉ
                        const diaChiField = document.getElementById('diaChi');
                        if (diaChiField) {
                            diaChiField.disabled = false;
                            diaChiField.readOnly = false;
                            console.log('🔓 Enabled diaChi field để cập nhật địa chỉ');
                            
                            // Gọi reverse geocode với targetElementId = 'diaChi'
                            if (typeof reverseGeocode === 'function') {
                                setTimeout(() => {
                                    reverseGeocode(lat, lng, 'diaChi');
                                }, 100);
                            }
                        }
                    };
                } else {
                    console.error('❌ Không thể khởi tạo bản đồ profile');
                    alert('Không thể khởi tạo bản đồ. Vui lòng kiểm tra kết nối internet và thử lại.');
                }
            } else {
                console.error('❌ window.mapsHelper chưa sẵn sàng');
                alert('Hệ thống bản đồ chưa sẵn sàng. Vui lòng refresh trang.');
            }
        }, 800);
        
    } else {
        if (editBtn) editBtn.style.display = 'inline-block';
        if (saveBtn) saveBtn.style.display = 'none';
        if (cancelBtn) cancelBtn.style.display = 'none';
        if (mapControls) mapControls.style.display = 'none';
        
        // Clear any validation errors when exiting edit mode
        clearFormErrors();
    }
}

// Cancel edit mode - FIXED VERSION
function cancelEdit() {
    if (currentUser) {
        // Reset form values to original data
        const tenField = document.getElementById('tenNguoiDung');
        const sdtField = document.getElementById('sdt');
        const diaChiField = document.getElementById('diaChi');
        
        if (tenField) tenField.value = currentUser.tenNguoiDung || '';
        if (sdtField) sdtField.value = currentUser.sdt || '';
        if (diaChiField) diaChiField.value = currentUser.diaChi || '';
        
        // Clear errors
        clearFormErrors();
        
        // Manually reset the UI without calling toggleEditMode
        const editableFields = ['tenNguoiDung', 'sdt', 'diaChi'];
        const editBtn = document.getElementById('editProfileBtn');
        const saveBtn = document.getElementById('saveProfileBtn');
        const cancelBtn = document.getElementById('cancelProfileBtn');
        const mapControls = document.getElementById('profileMapControls');
        
        // Disable all fields
        editableFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.disabled = true;
            }
        });
        
        // Show edit button, hide save and cancel buttons
        if (editBtn) editBtn.style.display = 'inline-block';
        if (saveBtn) saveBtn.style.display = 'none';
        if (cancelBtn) cancelBtn.style.display = 'none';
        if (mapControls) mapControls.style.display = 'none';
        
        // Reset edit mode flag
        isEditMode = false;
        
        console.log('Cancel edit: UI reset completed');
    }
}
// Save profile
async function saveProfile() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    // Validate form
    if (!validateProfileForm()) {
        return;
    }

    try {
        const formData = {
            tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
            sdt: document.getElementById('sdt').value.trim(),
            diaChi: document.getElementById('diaChi').value.trim()
        };

        const response = await fetch('/profile/api/user/update', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            isEditMode = false;
            toggleEditMode();
            showToast('Thành công!', 'Cập nhật thông tin thành công!', 'success', 3000);
            
            // Update localStorage user data
            const userData = localStorage.getItem('user');
            if (userData) {
                const user = JSON.parse(userData);
                user.username = formData.tenNguoiDung;
                localStorage.setItem('user', JSON.stringify(user));
            }
            
            // Reload page after 1.5 seconds
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('Save profile error:', error);
        showToast('Lỗi!', 'Lỗi khi lưu thông tin: ' + error.message, 'error');
    }
}

// Change password
async function changePassword() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    // Validate password form
    if (!validatePasswordForm()) {
        return;
    }

    try {
        const formData = {
            currentPassword: document.getElementById('currentPassword').value,
            newPassword: document.getElementById('newPassword').value
        };

        const response = await fetch('/profile/api/user/change-password', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            resetPasswordForm();
            showToast('Thành công!', 'Đổi mật khẩu thành công!', 'success', 3000);
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error', 3000);
        }

    } catch (error) {
        console.error('Change password error:', error);
        showToast('Lỗi!', 'Lỗi khi đổi mật khẩu: ' + error.message, 'error');
    }
}

// Validate profile form
function validateProfileForm() {
    let isValid = true;
    clearFormErrors();

    const tenNguoiDung = document.getElementById('tenNguoiDung').value.trim();
    const sdt = document.getElementById('sdt').value.trim();

    // Validate required fields
    if (!tenNguoiDung) {
        showFieldError('tenNguoiDung', 'Vui lòng nhập họ và tên');
        isValid = false;
    }

    // Validate phone number format
    if (sdt && !validatePhoneNumber(document.getElementById('sdt'))) {
        isValid = false;
    }

    return isValid;
}

// Validate password form
function validatePasswordForm() {
    let isValid = true;
    clearFormErrors();

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!currentPassword) {
        showFieldError('currentPassword', 'Vui lòng nhập mật khẩu hiện tại');
        isValid = false;
    }

    if (!newPassword) {
        showFieldError('newPassword', 'Vui lòng nhập mật khẩu mới');
        isValid = false;
    } else if (newPassword.length < 6) {
        showFieldError('newPassword', 'Mật khẩu phải có ít nhất 6 ký tự');
        isValid = false;
    }

    if (newPassword !== confirmPassword) {
        showFieldError('confirmPassword', 'Mật khẩu xác nhận không khớp');
        isValid = false;
    }

    return isValid;
}

// Show field error
function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const errorDiv = document.getElementById(fieldId + 'Error');
    
    if (field) {
        field.classList.add('error');
    }
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

// Clear form errors
function clearFormErrors() {
    document.querySelectorAll('.form-error').forEach(error => {
        error.style.display = 'none';
        error.textContent = '';
    });
    document.querySelectorAll('.form-input').forEach(input => {
        input.classList.remove('error');
    });
}

// Reset password form
function resetPasswordForm() {
    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.reset();
        clearFormErrors();
    }
}

// Toggle password visibility
function togglePasswordVisibility(toggleElement) {
    const passwordInput = toggleElement.parentElement.querySelector('input');
    if (passwordInput) {
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            toggleElement.className = 'fas fa-eye-slash toggle-password';
        } else {
            passwordInput.type = 'password';
            toggleElement.className = 'fas fa-eye toggle-password';
        }
    }
}

// Handle avatar upload
function handleAvatarUpload(input) {
    const file = input.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('avatarPreview');
            if (preview) {
                preview.src = e.target.result;
            }
        };
        reader.readAsDataURL(file);
        
        // Upload avatar
        uploadAvatar(file);
    }
}

// Upload avatar
async function uploadAvatar(file) {
    console.log('=== FRONTEND UPLOAD AVATAR START ===');
    console.log('File info:', {
        name: file.name,
        size: file.size,
        type: file.type,
        lastModified: file.lastModified
    });

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('avatar', file);
        
        console.log('Sending request to /profile/api/user/upload-avatar');
        console.log('Token:', token.substring(0, 20) + '...');

        const response = await fetch('/profile/api/user/upload-avatar', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });

        console.log('Response status:', response.status);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));

        const result = await response.json();
        console.log('Response body:', result);

        if (result.success) {
            console.log('Upload successful');
            showToast('Thành công!', 'Cập nhật ảnh đại diện thành công!', 'success', 3000);
            // Refresh profile data
            console.log('Refreshing profile data...');
            await loadUserProfile();
            console.log('=== FRONTEND UPLOAD AVATAR SUCCESS ===');
        } else {
            console.error('Upload failed:', result.message);
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('=== FRONTEND UPLOAD AVATAR ERROR ===');
        console.error('Upload avatar error:', error);
        console.error('Error stack:', error.stack);
        showToast('Lỗi!', 'Lỗi khi tải ảnh lên: ' + error.message, 'error');
    }
}

// Show toast notification
function showToast(title, message, type = 'success', duration = 3000) {
    // Remove existing toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icon = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle';
    
    toast.innerHTML = `
        <i class="fas ${icon} toast-icon"></i>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close">
            <i class="fas fa-times"></i>
        </button>
    `;

    document.body.appendChild(toast);

    // Show toast with animation
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);

    // Auto hide after duration
    const autoHide = setTimeout(() => {
        hideToast(toast);
    }, duration);

    // Close button event
    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
        clearTimeout(autoHide);
        hideToast(toast);
    });
}

// Hide toast with animation
function hideToast(toast) {
    toast.classList.remove('show');
    toast.classList.add('hide');
    
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 300);
}

// ============= QUẢN LÝ ĐỊA CHỈ =============
let addresses = [];

// Load addresses
async function loadAddresses() {
    const token = localStorage.getItem('jwtToken');
    
    try {
        const response = await fetch('/profile/api/user/addresses', {
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) throw new Error('Failed to load addresses');

        const result = await response.json();
        if (result.success) {
            addresses = result.data;
            displayAddresses();
        }
    } catch (error) {
        console.error('Error loading addresses:', error);
        showNotification('Lỗi khi tải danh sách địa chỉ', 'error');
    }
}

// Display addresses
function displayAddresses() {
    const container = document.getElementById('addressesContainer');
    if (!container) return;

    if (addresses.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-map-marker-alt"></i>
                <p>Bạn chưa có địa chỉ giao hàng nào</p>
                <button class="btn btn-primary" onclick="showAddAddressModal()">
                    <i class="fas fa-plus"></i> Thêm địa chỉ mới
                </button>
            </div>
        `;
        return;
    }

    let html = '<div class="addresses-list">';
    addresses.forEach(addr => {
        // Không hiển thị map ở đây - chỉ hiển thị thông tin địa chỉ
        html += `
            <div class="address-card ${addr.macDinh ? 'default' : ''}">
                <div class="address-header">
                    <h4>${escapeHtml(addr.tenNguoiNhan)}</h4>
                    ${addr.macDinh ? '<span class="badge-default">Mặc định</span>' : ''}
                </div>
                <p class="address-phone"><i class="fas fa-phone"></i> ${escapeHtml(addr.soDienThoai)}</p>
                <p class="address-detail"><i class="fas fa-map-marker-alt"></i> ${escapeHtml(addr.diaChiChiTiet)}</p>
                <div class="address-actions">
                    <button class="btn btn-sm btn-secondary" onclick="editAddress(${addr.maDiaChi})">
                        <i class="fas fa-edit"></i> Sửa
                    </button>
                    ${!addr.macDinh ? `
                        <button class="btn btn-sm btn-success" onclick="setDefaultAddress(${addr.maDiaChi})">
                            <i class="fas fa-star"></i> Đặt làm mặc định
                        </button>
                    ` : ''}
                    <button class="btn btn-sm btn-danger" onclick="deleteAddress(${addr.maDiaChi})">
                        <i class="fas fa-trash"></i> Xóa
                    </button>
                </div>
            </div>
        `;
    });
    html += '</div>';
    html += `
        <button class="btn btn-primary" onclick="showAddAddressModal()" style="margin-top: 20px;">
            <i class="fas fa-plus"></i> Thêm địa chỉ mới
        </button>
    `;
    
    container.innerHTML = html;
    
    // KHÔNG hiển thị bản đồ ở đây nữa
    // Map chỉ hiển thị khi thêm/sửa địa chỉ trong modal
}

// Show add address modal
function showAddAddressModal() {
    console.log('🏠 Mở modal thêm địa chỉ mới');
    const modal = document.getElementById('addressModal');
    if (!modal) {
        console.error('❌ Không tìm thấy addressModal');
        return;
    }
    
    document.getElementById('addressModalTitle').textContent = 'Thêm địa chỉ mới';
    document.getElementById('addressForm').reset();
    document.getElementById('addressId').value = '';
    modal.style.display = 'flex';
    
    // Khởi tạo OpenStreetMap sau khi modal hiển thị
    setTimeout(() => {
        console.log('🗺️ Đang khởi tạo bản đồ...');
        if (typeof L === 'undefined') {
            console.error('❌ Leaflet chưa được load!');
            alert('Thư viện bản đồ chưa được tải. Vui lòng refresh trang và thử lại.');
            return;
        }
        
        if (window.mapsHelper) {
            const mapInstance = window.mapsHelper.initMap('map', 10.762622, 106.660172);
            if (mapInstance) {
                console.log('✅ Bản đồ đã được khởi tạo thành công');
                window.mapsHelper.initAutocomplete('diaChiChiTiet');
            } else {
                console.error('❌ Không thể khởi tạo bản đồ');
            }
        } else {
            console.error('❌ window.mapsHelper chưa sẵn sàng');
            alert('Hệ thống bản đồ chưa sẵn sàng. Vui lòng refresh trang và thử lại.');
        }
    }, 500); // Tăng timeout lên 500ms để đảm bảo modal đã hiển thị hoàn toàn
}

// Edit address
function editAddress(id) {
    console.log('✏️ Sửa địa chỉ ID:', id);
    const addr = addresses.find(a => a.maDiaChi === id);
    if (!addr) {
        console.error('❌ Không tìm thấy địa chỉ với ID:', id);
        return;
    }

    document.getElementById('addressModalTitle').textContent = 'Chỉnh sửa địa chỉ';
    document.getElementById('addressId').value = addr.maDiaChi;
    document.getElementById('tenNguoiNhan').value = addr.tenNguoiNhan;
    document.getElementById('soDienThoaiAddress').value = addr.soDienThoai;
    document.getElementById('diaChiChiTiet').value = addr.diaChiChiTiet;
    document.getElementById('latitude').value = addr.latitude || '';
    document.getElementById('longitude').value = addr.longitude || '';
    
    document.getElementById('addressModal').style.display = 'flex';
    
    // Khởi tạo OpenStreetMap với vị trí của địa chỉ
    setTimeout(() => {
        console.log('🗺️ Đang khởi tạo bản đồ cho địa chỉ hiện tại...');
        if (typeof L === 'undefined') {
            console.error('❌ Leaflet chưa được load!');
            alert('Thư viện bản đồ chưa được tải. Vui lòng refresh trang và thử lại.');
            return;
        }
        
        if (window.mapsHelper) {
            const lat = addr.latitude || 10.762622;
            const lng = addr.longitude || 106.660172;
            console.log('📍 Tọa độ:', lat, lng);
            
            const mapInstance = window.mapsHelper.initMap('map', lat, lng);
            if (mapInstance) {
                console.log('✅ Bản đồ đã được khởi tạo thành công');
                window.mapsHelper.initAutocomplete('diaChiChiTiet');
            } else {
                console.error('❌ Không thể khởi tạo bản đồ');
            }
        } else {
            console.error('❌ window.mapsHelper chưa sẵn sàng');
            alert('Hệ thống bản đồ chưa sẵn sàng. Vui lòng refresh trang và thử lại.');
        }
    }, 500);
}

// Save address
async function saveAddress() {
    const form = document.getElementById('addressForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const addressId = document.getElementById('addressId').value;
    const latitude = document.getElementById('latitude').value;
    const longitude = document.getElementById('longitude').value;
    
    const data = {
        tenNguoiNhan: document.getElementById('tenNguoiNhan').value.trim(),
        soDienThoai: document.getElementById('soDienThoaiAddress').value.trim(),
        diaChiChiTiet: document.getElementById('diaChiChiTiet').value.trim(),
        latitude: latitude ? parseFloat(latitude) : null,
        longitude: longitude ? parseFloat(longitude) : null
    };

    const token = localStorage.getItem('jwtToken');
    const url = addressId 
        ? `/profile/api/user/addresses/${addressId}` 
        : '/profile/api/user/addresses';
    const method = addressId ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();
        if (result.success) {
            showToast('Thành công!', addressId ? 'Cập nhật địa chỉ thành công' : 'Thêm địa chỉ thành công', 'success', 3000);
            closeAddressModal();
            loadAddresses();
        } else {
            showToast('Lỗi!', result.message || 'Có lỗi xảy ra', 'error');
        }
    } catch (error) {
        console.error('Error saving address:', error);
        showToast('Lỗi!', 'Lỗi khi lưu địa chỉ', 'error');
    }
}

// Set default address
async function setDefaultAddress(id) {
    const token = localStorage.getItem('jwtToken');
    
    try {
        const response = await fetch(`/profile/api/user/addresses/${id}/set-default`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        if (result.success) {
            showToast('Thành công!', 'Đặt địa chỉ mặc định thành công', 'success');
            loadAddresses();
        } else {
            showToast('Lỗi!', result.message || 'Có lỗi xảy ra', 'error');
        }
    } catch (error) {
        console.error('Error setting default address:', error);
        showToast('Lỗi!', 'Lỗi khi đặt địa chỉ mặc định', 'error');
    }
}

// Delete address
async function deleteAddress(id) {
    if (!confirm('Bạn có chắc muốn xóa địa chỉ này?')) return;

    const token = localStorage.getItem('jwtToken');
    
    try {
        const response = await fetch(`/profile/api/user/addresses/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        if (result.success) {
            showToast('Thành công!', 'Xóa địa chỉ thành công', 'success');
            loadAddresses();
        } else {
            showToast('Lỗi!', result.message || 'Có lỗi xảy ra', 'error');
        }
    } catch (error) {
        console.error('Error deleting address:', error);
        showToast('Lỗi!', 'Lỗi khi xóa địa chỉ', 'error');
    }
}

// Close address modal
function closeAddressModal() {
    document.getElementById('addressModal').style.display = 'none';
    document.getElementById('addressForm').reset();
}

// Tìm địa chỉ trên bản đồ
function searchAddressOnMap() {
    console.log('🔍 Tìm địa chỉ trên bản đồ...');
    const addressInput = document.getElementById('diaChiChiTiet');
    const address = addressInput ? addressInput.value.trim() : '';
    
    if (!address) {
        showToast('Thông báo', 'Vui lòng nhập địa chỉ trước', 'error');
        return;
    }
    
    if (window.mapsHelper && window.mapsHelper.geocodeAddress) {
        console.log('📍 Đang tìm kiếm:', address);
        window.mapsHelper.geocodeAddress(address, (coords) => {
            console.log('✅ Tìm thấy tọa độ:', coords);
            showToast('Thành công', 'Đã tìm thấy địa chỉ trên bản đồ', 'success');
        });
    } else {
        console.error('❌ Hệ thống bản đồ chưa sẵn sàng');
        showToast('Lỗi', 'Hệ thống bản đồ chưa sẵn sàng', 'error');
    }
}

// Lấy vị trí hiện tại
function getCurrentLocationForAddress() {
    console.log('📍 Đang lấy vị trí hiện tại...');
    if (window.mapsHelper && window.mapsHelper.getCurrentLocation) {
        window.mapsHelper.getCurrentLocation((coords) => {
            console.log('✅ Đã lấy vị trí hiện tại:', coords);
            showToast('Thành công', 'Đã lấy vị trí hiện tại', 'success');
        });
    } else {
        console.error('❌ Hệ thống bản đồ chưa sẵn sàng');
        showToast('Lỗi', 'Không thể lấy vị trí hiện tại', 'error');
    }
}

// ============= PROFILE MAP FUNCTIONS =============

// Tìm địa chỉ trên bản đồ profile
function searchProfileAddressOnMap() {
    console.log('🔍 Tìm địa chỉ profile trên bản đồ...');
    const addressInput = document.getElementById('diaChi');
    const address = addressInput ? addressInput.value.trim() : '';
    
    if (!address) {
        showToast('Thông báo', 'Vui lòng nhập địa chỉ trước', 'error');
        return;
    }
    
    if (window.mapsHelper && window.mapsHelper.geocodeAddress) {
        console.log('📍 Đang tìm kiếm địa chỉ profile:', address);
        window.mapsHelper.geocodeAddress(address, (coords) => {
            console.log('✅ Tìm thấy tọa độ profile:', coords);
            if (window.updateProfileCoordinates) {
                window.updateProfileCoordinates(coords.lat, coords.lng);
            }
            showToast('Thành công', 'Đã tìm thấy địa chỉ trên bản đồ', 'success');
        });
    } else {
        console.error('❌ Hệ thống bản đồ chưa sẵn sàng');
        showToast('Lỗi', 'Hệ thống bản đồ chưa sẵn sàng', 'error');
    }
}

// Lấy vị trí hiện tại cho profile
function getProfileCurrentLocation() {
    console.log('📍 Đang lấy vị trí hiện tại cho profile...');
    if (window.mapsHelper && window.mapsHelper.getCurrentLocation) {
        window.mapsHelper.getCurrentLocation((coords) => {
            console.log('✅ Đã lấy vị trí hiện tại profile:', coords);
            if (window.updateProfileCoordinates) {
                window.updateProfileCoordinates(coords.lat, coords.lng);
            }
            showToast('Thành công', 'Đã lấy vị trí hiện tại', 'success');
        });
    } else {
        console.error('❌ Hệ thống bản đồ chưa sẵn sàng');
        showToast('Lỗi', 'Không thể lấy vị trí hiện tại', 'error');
    }
}