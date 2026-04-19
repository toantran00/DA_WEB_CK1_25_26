// Leaflet Maps Integration - KHÔNG CẦN API KEY
// Thay thế Google Maps bằng OpenStreetMap (miễn phí 100%)

// Lưu trữ các map instances theo containerId
const mapInstances = {};
const markerInstances = {};

let map = null; // Giữ lại cho backward compatibility
let marker = null; // Giữ lại cho backward compatibility

// Khởi tạo Leaflet Map
function initMap(containerId = 'map', lat = 10.762622, lng = 106.660172) {
    try {
        console.log('🗺️ Đang khởi tạo bản đồ cho container:', containerId);
        
        // Kiểm tra Leaflet đã được load chưa
        if (typeof L === 'undefined') {
            console.error('❌ Leaflet library chưa được load!');
            setTimeout(() => initMap(containerId, lat, lng), 500);
            return null;
        }
        
        // Kiểm tra container có tồn tại không
        const container = document.getElementById(containerId);
        if (!container) {
            console.error('❌ Không tìm thấy container:', containerId);
            return null;
        }
        
        console.log('📦 Container found:', container);
        console.log('📦 Container dimensions:', container.offsetWidth, 'x', container.offsetHeight);
        
        // Nếu map đã tồn tại cho container này, xóa đi
        if (mapInstances[containerId]) {
            console.log('🔄 Xóa bản đồ cũ của container:', containerId);
            mapInstances[containerId].remove();
            delete mapInstances[containerId];
            delete markerInstances[containerId];
        }
        
        // Xóa innerHTML của container (phòng trường hợp map cũ còn sót)
        container.innerHTML = '';
        
        // Đảm bảo container có kích thước
        if (container.offsetHeight === 0) {
            console.warn('⚠️ Container height = 0, setting min-height...');
            container.style.minHeight = '300px';
        }
        
        console.log('🌍 Khởi tạo Leaflet map...');
        // Khởi tạo bản đồ với OpenStreetMap
        const newMap = L.map(containerId, {
            center: [lat, lng],
            zoom: 15,
            zoomControl: true,
            attributionControl: true
        });

        console.log('🗺️ Thêm tile layer...');
        // Thêm tile layer từ OpenStreetMap (miễn phí, không cần API key)
        const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19,
            minZoom: 5
        });
        
        tileLayer.on('tileerror', function(error) {
            console.error('❌ Tile loading error:', error);
        });
        
        tileLayer.on('tileload', function() {
            console.log('✅ Tiles đang load...');
        });
        
        tileLayer.addTo(newMap);

        console.log('📍 Thêm marker...');
        // Tạo marker
        const newMarker = L.marker([lat, lng], {
            draggable: true
        }).addTo(newMap);

        // Lưu map và marker instance
        mapInstances[containerId] = newMap;
        markerInstances[containerId] = newMarker;
        
        // Cập nhật biến global (cho backward compatibility)
        map = newMap;
        marker = newMarker;

        // Lắng nghe sự kiện kéo marker
        newMarker.on('dragend', function(event) {
            const position = newMarker.getLatLng();
            console.log('🎯 Marker dragged to:', position.lat, position.lng, 'on container:', containerId);
            
            // Xác định element ID dựa trên containerId
            const targetElementId = containerId === 'profileMap' ? 'diaChi' : 'diaChiChiTiet';
            
            // FORCE enable textarea để cập nhật địa chỉ
            const addressInput = document.getElementById(targetElementId);
            
            if (addressInput) {
                addressInput.disabled = false;
                addressInput.readOnly = false;
                console.log('🔓 FORCE enable textarea:', targetElementId);
            }
            
            updateCoordinates(position.lat, position.lng);
            reverseGeocode(position.lat, position.lng, targetElementId);
        });

        // Click vào map để đặt marker mới
        newMap.on('click', function(e) {
            placeMarkerOnMap(e.latlng, containerId);
        });

        // Force resize map sau khi khởi tạo
        setTimeout(() => {
            if (newMap) {
                console.log('🔄 Invalidating map size...');
                newMap.invalidateSize();
            }
        }, 100);

        console.log('✅ Bản đồ OpenStreetMap đã load thành công!');
        return newMap;
    } catch (error) {
        console.error('❌ Lỗi khởi tạo bản đồ:', error);
        console.error('Error stack:', error.stack);
        return null;
    }
}

// Đặt marker tại vị trí (backward compatibility)
function placeMarker(latlng) {
    placeMarkerOnMap(latlng, 'map');
}

// Đặt marker trên map cụ thể
function placeMarkerOnMap(latlng, containerId = 'map') {
    console.log('📍 Placing marker at:', latlng.lat, latlng.lng, 'on container:', containerId);
    
    const mapInstance = mapInstances[containerId] || map;
    const markerInstance = markerInstances[containerId] || marker;
    
    if (!mapInstance) {
        console.error('❌ Map instance not found for:', containerId);
        return;
    }
    
    // Xác định element ID dựa trên containerId
    const targetElementId = containerId === 'profileMap' ? 'diaChi' : 'diaChiChiTiet';
    
    if (markerInstance) {
        markerInstance.setLatLng(latlng);
    } else {
        const newMarker = L.marker(latlng, { draggable: true }).addTo(mapInstance);
        markerInstances[containerId] = newMarker;
        
        if (containerId === 'map') {
            marker = newMarker; // Update global for backward compatibility
        }
        
        // Add dragend event to new marker
        newMarker.on('dragend', function(event) {
            const position = newMarker.getLatLng();
            console.log('🎯 Marker dragged to:', position.lat, position.lng);
            
            // Xác định element ID cho container này
            const dragTargetElementId = containerId === 'profileMap' ? 'diaChi' : 'diaChiChiTiet';
            
            // FORCE enable textarea để cập nhật địa chỉ
            const addressInput = document.getElementById(dragTargetElementId);
            
            if (addressInput) {
                addressInput.disabled = false;
                addressInput.readOnly = false;
                console.log('🔓 FORCE enable textarea:', dragTargetElementId);
            }
            
            updateCoordinates(position.lat, position.lng);
            reverseGeocode(position.lat, position.lng, dragTargetElementId);
        });
    }
    
    mapInstance.panTo(latlng);
    
    // FORCE enable textarea trước khi cập nhật
    const addressInput = document.getElementById(targetElementId);
    
    if (addressInput) {
        addressInput.disabled = false;
        addressInput.readOnly = false;
        console.log('🔓 FORCE enable textarea:', targetElementId);
    }
    
    updateCoordinates(latlng.lat, latlng.lng);
    reverseGeocode(latlng.lat, latlng.lng, targetElementId);
}

// Reverse Geocoding - chuyển tọa độ thành địa chỉ
function reverseGeocode(lat, lng, targetElementId = null) {
    console.log('=== REVERSE GEOCODING START ===');
    console.log('🔄 Tọa độ:', lat, lng);
    console.log('🎯 Target element ID:', targetElementId);
    
    // Tìm input địa chỉ - ưu tiên targetElementId nếu được chỉ định
    let addressInput = null;
    
    if (targetElementId) {
        addressInput = document.getElementById(targetElementId);
        console.log('🔍 Tìm kiếm element theo targetElementId:', targetElementId, '→', addressInput);
    }
    
    // Nếu không tìm thấy theo targetElementId, tìm theo thứ tự mặc định
    if (!addressInput) {
        addressInput = document.getElementById('diaChiChiTiet') || 
                       document.getElementById('diaChi');
        console.log('🔍 Tìm kiếm element mặc định:', addressInput ? addressInput.id : 'null');
    }
    
    if (!addressInput) {
        console.error('❌ KHÔNG TÌM THẤY textarea/input địa chỉ!');
        console.log('📋 Tất cả textarea trong document:');
        document.querySelectorAll('textarea').forEach(ta => {
            const val = ta.value || '';
            console.log('  - ID:', ta.id, 'Name:', ta.name, 'Value:', val.substring(0, 50));
        });
        return;
    }
    
    const currentValue = addressInput.value || '';
    console.log('✅ Tìm thấy element:', addressInput.tagName, 'ID:', addressInput.id);
    console.log('📦 Thuộc tính:', {
        disabled: addressInput.disabled,
        readOnly: addressInput.readOnly,
        value: currentValue.substring(0, 50) + (currentValue.length > 50 ? '...' : '')
    });
    
    // FORCE enable textarea để cập nhật được - QUAN TRỌNG!
    addressInput.disabled = false;
    addressInput.readOnly = false;
    console.log('🔓 ĐÃ FORCE ENABLE textarea để cập nhật');
    
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1`;
    console.log('📡 Gọi API:', url);
    
    fetch(url)
        .then(response => {
            console.log('📥 Response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('📍 Dữ liệu nhận được:', data);
            
            if (data && data.display_name) {
                const oldValue = addressInput.value || '';
                console.log('📝 Địa chỉ CŨ:', oldValue);
                console.log('📝 Địa chỉ MỚI:', data.display_name);
                
                // FORCE enable lần nữa để chắc chắn
                addressInput.disabled = false;
                addressInput.readOnly = false;
                
                // CẬP NHẬT GIÁ TRỊ - thử nhiều cách
                addressInput.value = data.display_name;
                addressInput.textContent = data.display_name;
                addressInput.innerHTML = data.display_name;
                
                console.log('📝 Giá trị SAU KHI cập nhật:', addressInput.value);
                console.log('✅ Đã set value thành công:', addressInput.value === data.display_name);
                
                // Trigger events để form biết có thay đổi
                const inputEvent = new Event('input', { bubbles: true, cancelable: true });
                const changeEvent = new Event('change', { bubbles: true, cancelable: true });
                addressInput.dispatchEvent(inputEvent);
                addressInput.dispatchEvent(changeEvent);
                
                // Focus vào textarea để user thấy rõ đã cập nhật
                addressInput.focus();
                
                console.log('=== REVERSE GEOCODING SUCCESS ===');
            } else {
                console.warn('⚠️ API không trả về display_name');
                console.log('Response data:', data);
            }
        })
        .catch(error => {
            console.error('❌ LỖI reverse geocoding:', error);
            console.error('Error stack:', error.stack);
        });
}

// Geocoding - chuyển địa chỉ thành tọa độ
function geocodeAddress(address, callback) {
    if (!address || address.trim() === '') {
        alert('Vui lòng nhập địa chỉ');
        return;
    }

    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}&countrycodes=vn&limit=1`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                const lat = parseFloat(data[0].lat);
                const lng = parseFloat(data[0].lon);
                
                // Tìm map instance hiện tại (ưu tiên profileMap, rồi đến map)
                const activeMap = mapInstances['profileMap'] || mapInstances['map'] || map;
                const activeMarker = markerInstances['profileMap'] || markerInstances['map'] || marker;
                const containerId = mapInstances['profileMap'] ? 'profileMap' : 'map';
                const targetElementId = containerId === 'profileMap' ? 'diaChi' : 'diaChiChiTiet';
                
                if (activeMap) {
                    const latlng = L.latLng(lat, lng);
                    activeMap.setView(latlng, 15);
                    if (activeMarker) {
                        activeMarker.setLatLng(latlng);
                    } else {
                        const newMarker = L.marker(latlng, { draggable: true }).addTo(activeMap);
                        markerInstances[containerId] = newMarker;
                        marker = newMarker;
                    }
                }
                
                updateCoordinates(lat, lng);
                reverseGeocode(lat, lng, targetElementId); // Truyền targetElementId
                
                if (callback) {
                    callback({ lat, lng });
                }
            } else {
                alert('Không tìm thấy địa chỉ. Vui lòng thử lại với địa chỉ khác.');
            }
        })
        .catch(error => {
            console.error('Lỗi geocoding:', error);
            alert('Không thể tìm kiếm địa chỉ. Vui lòng thử lại.');
        });
}

// Cập nhật tọa độ vào hidden fields
function updateCoordinates(lat, lng) {
    // Cập nhật cho địa chỉ giao hàng (modal)
    const latInput = document.getElementById('latitude');
    const lngInput = document.getElementById('longitude');
    
    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;
    
    // Cập nhật cho profile map (nếu có callback)
    if (window.updateProfileCoordinates && typeof window.updateProfileCoordinates === 'function') {
        window.updateProfileCoordinates(lat, lng);
    }
    
    console.log('Tọa độ đã cập nhật:', lat, lng);
}

// Khởi tạo autocomplete (simplified version)
function initAutocomplete(inputId = 'diaChiChiTiet') {
    const input = document.getElementById(inputId);
    if (!input) return;

    let timeout = null;
    input.addEventListener('input', function() {
        clearTimeout(timeout);
        const query = this.value;
        
        if (query.length < 3) return;
        
        timeout = setTimeout(() => {
            searchAddress(query);
        }, 500);
    });
}

// Tìm địa chỉ
function searchAddress(query) {
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&countrycodes=vn&limit=5`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            // Có thể hiển thị suggestions ở đây
            console.log('Kết quả tìm kiếm:', data);
        })
        .catch(error => {
            console.error('Lỗi tìm kiếm:', error);
        });
}

// Lấy vị trí hiện tại
function getCurrentLocation(callback) {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                
                // Tìm map instance hiện tại (ưu tiên profileMap, rồi đến map)
                const activeMap = mapInstances['profileMap'] || mapInstances['map'] || map;
                const activeMarker = markerInstances['profileMap'] || markerInstances['map'] || marker;
                const containerId = mapInstances['profileMap'] ? 'profileMap' : 'map';
                const targetElementId = containerId === 'profileMap' ? 'diaChi' : 'diaChiChiTiet';
                
                if (activeMap) {
                    const latlng = L.latLng(lat, lng);
                    activeMap.setView(latlng, 15);
                    if (activeMarker) {
                        activeMarker.setLatLng(latlng);
                    } else {
                        const newMarker = L.marker(latlng, { draggable: true }).addTo(activeMap);
                        markerInstances[containerId] = newMarker;
                        marker = newMarker;
                    }
                }
                
                updateCoordinates(lat, lng);
                reverseGeocode(lat, lng, targetElementId);
                
                if (callback) {
                    callback({ lat, lng });
                }
            },
            (error) => {
                console.error('Lỗi lấy vị trí:', error);
                alert('Không thể lấy vị trí hiện tại. Vui lòng cho phép truy cập vị trí.');
            }
        );
    } else {
        alert('Trình duyệt của bạn không hỗ trợ Geolocation');
    }
}

// Hiển thị bản đồ cho địa chỉ (chỉ xem)
function showAddressOnMap(address, lat, lng, containerId = 'map') {
    try {
        console.log('🗺️ Hiển thị bản đồ cho địa chỉ:', containerId);
        
        const container = document.getElementById(containerId);
        if (!container) {
            console.error('❌ Không tìm thấy container:', containerId);
            return null;
        }
        
        // Xóa nội dung cũ nếu có
        container.innerHTML = '';
        
        const viewMap = L.map(containerId).setView([lat || 10.762622, lng || 106.660172], 15);
        
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(viewMap);
        
        const viewMarker = L.marker([lat || 10.762622, lng || 106.660172]).addTo(viewMap);
        
        if (address) {
            viewMarker.bindPopup(address).openPopup();
        }
        
        // Force resize map
        setTimeout(() => {
            if (viewMap) {
                viewMap.invalidateSize();
            }
        }, 100);
        
        console.log('✅ Bản đồ xem địa chỉ đã load thành công!');
        return viewMap;
    } catch (error) {
        console.error('❌ Lỗi hiển thị bản đồ:', error);
        console.error('Error stack:', error.stack);
        return null;
    }
}

// Tìm kiếm địa chỉ trên bản đồ
function searchAddressOnMap() {
    const addressInput = document.getElementById('diaChiChiTiet') || 
                        document.getElementById('diaChi');
    const address = addressInput ? addressInput.value.trim() : '';
    
    if (!address) {
        alert('Vui lòng nhập địa chỉ trước');
        return;
    }
    
    geocodeAddress(address);
}

// Reset bản đồ
function resetMap() {
    const defaultLat = 10.762622;
    const defaultLng = 106.660172;
    
    if (map) {
        const latlng = L.latLng(defaultLat, defaultLng);
        map.setView(latlng, 15);
        if (marker) {
            marker.setLatLng(latlng);
        }
    }
    
    updateCoordinates(defaultLat, defaultLng);
}

// Export functions
window.mapsHelper = {
    initMap,
    initAutocomplete,
    getCurrentLocation,
    geocodeAddress,
    reverseGeocode,
    showAddressOnMap,
    searchAddress: searchAddressOnMap,
    resetMap,
    placeMarker,
    placeMarkerOnMap,
    updateCoordinates,
    mapInstances,
    markerInstances
};

console.log('✅ Maps helper (OpenStreetMap) đã sẵn sàng - KHÔNG CẦN API KEY!');
