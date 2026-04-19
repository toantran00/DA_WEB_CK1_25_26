// Google Maps Integration for Address Management
// Đây là file để tích hợp Google Maps vào các chức năng địa chỉ

let map = null;
let marker = null;
let geocoder = null;
let autocomplete = null;

// Global error handler for Google Maps
window.gm_authFailure = function() {
    console.error('❌ Google Maps Authentication Failed!');
    const errorMsg = `
        ⚠️ LỖI GOOGLE MAPS API KEY
        
        Nguyên nhân có thể:
        1. API Key chưa được thiết lập (vẫn là YOUR_API_KEY)
        2. API Key không đúng
        3. Chưa bật Maps JavaScript API
        4. Domain restriction không đúng
        
        Hướng dẫn sửa:
        - Đọc file: GOOGLE_MAPS_API_KEY_GUIDE.html
        - Hoặc tạm thời comment dòng Google Maps API trong HTML
    `;
    alert(errorMsg);
};

// Kiểm tra Google Maps API đã load chưa
function isGoogleMapsLoaded() {
    return typeof google !== 'undefined' && typeof google.maps !== 'undefined';
}

// Đợi Google Maps API load xong
function waitForGoogleMaps(callback, maxAttempts = 20) {
    let attempts = 0;
    const checkInterval = setInterval(function() {
        attempts++;
        if (isGoogleMapsLoaded()) {
            clearInterval(checkInterval);
            callback();
        } else if (attempts >= maxAttempts) {
            clearInterval(checkInterval);
            console.error('Google Maps API không load được sau 10 giây');
            alert('Không thể tải Google Maps. Vui lòng kiểm tra kết nối internet và API key.');
        }
    }, 500); // Check mỗi 500ms
}

// Khởi tạo Google Maps
function initMap(containerId = 'map', lat = 10.762622, lng = 106.660172) {
    if (!isGoogleMapsLoaded()) {
        console.warn('Google Maps API chưa load, đang đợi...');
        waitForGoogleMaps(function() {
            initMap(containerId, lat, lng);
        });
        return;
    }

    const mapOptions = {
        zoom: 15,
        center: { lat: lat, lng: lng },
        mapTypeControl: true,
        streetViewControl: true,
        fullscreenControl: true,
        zoomControl: true
    };

    map = new google.maps.Map(document.getElementById(containerId), mapOptions);
    geocoder = new google.maps.Geocoder();

    // Tạo marker
    marker = new google.maps.Marker({
        map: map,
        draggable: true,
        position: { lat: lat, lng: lng }
    });

    // Lắng nghe sự kiện kéo marker
    google.maps.event.addListener(marker, 'dragend', function(event) {
        const lat = event.latLng.lat();
        const lng = event.latLng.lng();
        
        // Cập nhật địa chỉ từ tọa độ
        geocodeLatLng(lat, lng);
        
        // Lưu tọa độ vào hidden fields
        updateCoordinates(lat, lng);
    });

    // Click vào map để đặt marker mới
    google.maps.event.addListener(map, 'click', function(event) {
        placeMarker(event.latLng);
    });

    return map;
}

// Đặt marker tại vị trí
function placeMarker(location) {
    marker.setPosition(location);
    map.panTo(location);
    
    const lat = location.lat();
    const lng = location.lng();
    
    geocodeLatLng(lat, lng);
    updateCoordinates(lat, lng);
}

// Chuyển đổi tọa độ thành địa chỉ
function geocodeLatLng(lat, lng) {
    const latlng = { lat: lat, lng: lng };
    
    geocoder.geocode({ location: latlng }, (results, status) => {
        if (status === 'OK') {
            if (results[0]) {
                const addressInput = document.getElementById('diaChiChiTiet');
                if (addressInput && !addressInput.value) {
                    addressInput.value = results[0].formatted_address;
                }
            }
        }
    });
}

// Chuyển đổi địa chỉ thành tọa độ
function geocodeAddress(address, callback) {
    if (!isGoogleMapsLoaded()) {
        console.error('Google Maps API chưa load');
        alert('Google Maps chưa sẵn sàng. Vui lòng thử lại sau vài giây.');
        return;
    }
    
    if (!geocoder) {
        geocoder = new google.maps.Geocoder();
    }
    
    geocoder.geocode({ address: address }, (results, status) => {
        if (status === 'OK' && results[0]) {
            const location = results[0].geometry.location;
            const lat = location.lat();
            const lng = location.lng();
            
            if (map) {
                map.setCenter(location);
                marker.setPosition(location);
            }
            
            updateCoordinates(lat, lng);
            
            if (callback) {
                callback({ lat, lng });
            }
        } else {
            console.error('Geocode không thành công: ' + status);
        }
    });
}

// Cập nhật tọa độ vào các trường ẩn
function updateCoordinates(lat, lng) {
    const latInput = document.getElementById('latitude');
    const lngInput = document.getElementById('longitude');
    
    if (latInput) latInput.value = lat;
    if (lngInput) lngInput.value = lng;
}

// Khởi tạo autocomplete cho ô nhập địa chỉ
function initAutocomplete(inputId = 'diaChiChiTiet') {
    if (!isGoogleMapsLoaded()) {
        console.warn('Google Maps API chưa load, đang đợi autocomplete...');
        waitForGoogleMaps(function() {
            initAutocomplete(inputId);
        });
        return;
    }
    
    const input = document.getElementById(inputId);
    if (!input) return;

    // Tạo autocomplete với ưu tiên khu vực Việt Nam
    autocomplete = new google.maps.places.Autocomplete(input, {
        componentRestrictions: { country: 'vn' },
        fields: ['formatted_address', 'geometry', 'name']
    });

    // Lắng nghe sự kiện chọn địa chỉ
    autocomplete.addListener('place_changed', function() {
        const place = autocomplete.getPlace();
        
        if (!place.geometry) {
            console.log("Không tìm thấy địa điểm");
            return;
        }

        const location = place.geometry.location;
        const lat = location.lat();
        const lng = location.lng();

        if (map) {
            map.setCenter(location);
            marker.setPosition(location);
        }

        updateCoordinates(lat, lng);
    });
}

// Tìm vị trí hiện tại của người dùng
function getCurrentLocation(callback) {
    if (!isGoogleMapsLoaded()) {
        console.error('Google Maps API chưa load');
        alert('Google Maps chưa sẵn sàng. Vui lòng thử lại sau vài giây.');
        return;
    }
    
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                
                if (map) {
                    const location = { lat: lat, lng: lng };
                    map.setCenter(location);
                    marker.setPosition(location);
                }
                
                geocodeLatLng(lat, lng);
                updateCoordinates(lat, lng);
                
                if (callback) {
                    callback({ lat, lng });
                }
            },
            (error) => {
                console.error('Lỗi lấy vị trí: ' + error.message);
                alert('Không thể lấy vị trí hiện tại của bạn. Vui lòng cho phép truy cập vị trí.');
            }
        );
    } else {
        alert('Trình duyệt của bạn không hỗ trợ Geolocation');
    }
}

// Hiển thị bản đồ cho một địa chỉ cụ thể (chỉ xem)
function showAddressOnMap(address, lat, lng, containerId = 'map') {
    if (!isGoogleMapsLoaded()) {
        console.warn('Google Maps API chưa load, đang đợi...');
        waitForGoogleMaps(function() {
            showAddressOnMap(address, lat, lng, containerId);
        });
        return;
    }
    
    const mapOptions = {
        zoom: 15,
        center: { lat: lat || 10.762622, lng: lng || 106.660172 },
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false,
        zoomControl: true
    };

    const viewMap = new google.maps.Map(document.getElementById(containerId), mapOptions);
    
    const viewMarker = new google.maps.Marker({
        map: viewMap,
        position: { lat: lat || 10.762622, lng: lng || 106.660172 },
        title: address
    });

    // Thêm info window
    const infoWindow = new google.maps.InfoWindow({
        content: `<div style="padding: 10px;"><strong>${address}</strong></div>`
    });

    viewMarker.addListener('click', () => {
        infoWindow.open(viewMap, viewMarker);
    });

    return viewMap;
}

// Tìm kiếm địa chỉ
function searchAddress() {
    const addressInput = document.getElementById('diaChiChiTiet');
    if (addressInput && addressInput.value) {
        geocodeAddress(addressInput.value);
    }
}

// Reset bản đồ về vị trí mặc định (Hồ Chí Minh)
function resetMap() {
    const defaultLat = 10.762622;
    const defaultLng = 106.660172;
    
    if (map) {
        const location = { lat: defaultLat, lng: defaultLng };
        map.setCenter(location);
        marker.setPosition(location);
    }
    
    updateCoordinates(defaultLat, defaultLng);
}

// Export functions for use in other files
window.mapsHelper = {
    initMap,
    initAutocomplete,
    getCurrentLocation,
    geocodeAddress,
    showAddressOnMap,
    searchAddress,
    resetMap,
    placeMarker,
    updateCoordinates
};
