// ============= SHIPPING METHODS PAGE JAVASCRIPT - FIXED =============

// Global variables
let currentMethodId = null;
let currentMethodName = null;
let selectedMethods = new Set();

// ============= INITIALIZATION =============
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - Initializing shipping methods management...');
    
    // Initialize selection controls
    updateSelectionControls();
    
    // Add event listeners to all checkboxes
    const methodCheckboxes = document.querySelectorAll('.method-checkbox');
    methodCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
    
    // Add event listener to select all checkbox
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            toggleSelectAll(this);
        });
    }
    
    // Modal cleanup listeners
    setupModalCleanup();
    
    console.log('Shipping methods management system fully initialized');
});

// ============= TOAST NOTIFICATIONS =============
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        console.error('Toast container not found');
        return;
    }

    const toastId = 'toast-' + Date.now();
    
    const iconClass = type === 'success' ? 'fa-check-circle' : 
                     type === 'error' ? 'fa-exclamation-circle' : 
                     'fa-info-circle';
    
    const toastHTML = `
        <div id="${toastId}" class="toast toast-${type}">
            <div class="toast-icon">
                <i class="fas ${iconClass}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close" onclick="closeToast('${toastId}')">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHTML);
    
    const toast = document.getElementById(toastId);
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        closeToast(toastId);
    }, 5000);
}

function closeToast(toastId) {
    const toast = document.getElementById(toastId);
    if (toast) {
        toast.classList.remove('show');
        toast.classList.add('hide');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }
}

// ============= SELECTION MANAGEMENT =============
function toggleSelectAll(checkbox) {
    const methodCheckboxes = document.querySelectorAll('.method-checkbox');
    methodCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedMethods.add(cb.value);
        } else {
            selectedMethods.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.method-checkbox:checked');
    selectedMethods = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    
    if (selectedMethods.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedMethods.size;
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
    }
    
    const totalCheckboxes = document.querySelectorAll('.method-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedMethods.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedMethods.size > 0 && selectedMethods.size < totalCheckboxes;
    }
    
    document.querySelectorAll('.shipping-methods-table tbody tr').forEach(row => {
        const checkbox = row.querySelector('.method-checkbox');
        if (checkbox && checkbox.checked) {
            row.classList.add('selected');
        } else {
            row.classList.remove('selected');
        }
    });
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.method-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedMethods.clear();
    updateSelectionControls();
}

// ============= DELETE MODAL FUNCTIONS =============
function showDeleteModal(button) {
    currentMethodId = button.getAttribute('data-method-id');
    currentMethodName = button.getAttribute('data-method-name');
    
    const methodNameElement = document.getElementById('methodNameToDelete');
    if (methodNameElement) {
        methodNameElement.textContent = currentMethodName;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteMethod(currentMethodId);
        };
    }
    
    const deleteModalElement = document.getElementById('deleteModal');
    if (deleteModalElement) {
        const deleteModal = new bootstrap.Modal(deleteModalElement);
        deleteModal.show();
    } else {
        console.error('Delete modal element not found');
    }
}

function deleteMethod(methodId) {
    if (!methodId) return;
    
    const deleteModalElement = document.getElementById('deleteModal');
    const deleteModal = bootstrap.Modal.getInstance(deleteModalElement);
    if (deleteModal) {
        deleteModal.hide();
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/shipping-methods/delete/${methodId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Có lỗi xảy ra khi xóa phương thức vận chuyển');
        }
        return response.text();
    })
    .then(() => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        showToast('success', 'Thành công', 'Đã xóa phương thức vận chuyển thành công!');
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    })
    .catch(error => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa phương thức vận chuyển!');
    })
    .finally(() => {
        currentMethodId = null;
        currentMethodName = null;
    });
}

function showBulkDeleteModal() {
    if (selectedMethods.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 phương thức để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedMethods.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedMethods.size;
    
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedMethods.forEach(methodId => {
            const methodRow = document.querySelector(`.method-checkbox[value="${methodId}"]`);
            if (methodRow) {
                const row = methodRow.closest('tr');
                const methodNameElement = row.querySelector('td:nth-child(3) strong');
                const methodName = methodNameElement ? methodNameElement.textContent.trim() : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `<i class="fas fa-shipping-fast me-2"></i>${methodName}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeleteMethods;
    }
    
    const bulkDeleteModalElement = document.getElementById('bulkDeleteModal');
    if (bulkDeleteModalElement) {
        const bulkDeleteModal = new bootstrap.Modal(bulkDeleteModalElement);
        bulkDeleteModal.show();
    }
}

function bulkDeleteMethods() {
    const bulkDeleteModalElement = document.getElementById('bulkDeleteModal');
    const bulkDeleteModal = bootstrap.Modal.getInstance(bulkDeleteModalElement);
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const methodIds = Array.from(selectedMethods);
    
    let successCount = 0;
    let errorCount = 0;
    const errors = [];
    
    const deletePromises = methodIds.map(methodId => {
        return fetch(`/admin/shipping-methods/delete/${methodId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.ok) {
                successCount++;
            } else {
                errorCount++;
                errors.push(`Lỗi khi xóa phương thức ID ${methodId}`);
            }
        })
        .catch(error => {
            errorCount++;
            errors.push(`Lỗi khi xóa phương thức ID ${methodId}: ${error.message}`);
        });
    });
    
    Promise.all(deletePromises).then(() => {
        if (successCount > 0) {
            showToast('success', 'Thành công', `Đã xóa ${successCount} phương thức vận chuyển thành công`);
        }
        
        if (errorCount > 0) {
            errors.forEach(error => {
                showToast('error', 'Lỗi xóa', error);
            });
        }
        
        setTimeout(() => {
            location.reload();
        }, 1500);
    });
}

// ============= STATUS MODAL FUNCTIONS =============
function showStatusModal(button) {
    currentMethodId = button.getAttribute('data-method-id');
    currentMethodName = button.getAttribute('data-method-name');
    const currentMethodStatus = button.getAttribute('data-current-status') === 'true';
    
    const methodNameElement = document.getElementById('methodNameForStatus');
    if (methodNameElement) {
        methodNameElement.textContent = currentMethodName;
    }
    
    const statusActive = document.getElementById('statusActive');
    const statusInactive = document.getElementById('statusInactive');
    
    if (currentMethodStatus) {
        if (statusActive) statusActive.checked = true;
    } else {
        if (statusInactive) statusInactive.checked = true;
    }
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateMethodStatus;
    }
    
    const statusModalElement = document.getElementById('statusModal');
    if (statusModalElement) {
        const statusModal = new bootstrap.Modal(statusModalElement);
        statusModal.show();
    } else {
        console.error('Status modal element not found');
    }
}

function updateMethodStatus() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const statusModalElement = document.getElementById('statusModal');
    const statusModal = bootstrap.Modal.getInstance(statusModalElement);
    if (statusModal) {
        statusModal.hide();
    }
    
    fetch(`/admin/shipping-methods/toggle-status/${currentMethodId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Có lỗi xảy ra khi cập nhật trạng thái');
        }
        return response.text();
    })
    .then(() => {
        showToast('success', 'Thành công', 'Thay đổi trạng thái thành công');
        setTimeout(() => {
            location.reload();
        }, 1000);
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi cập nhật trạng thái');
    });
}

function showBulkStatusModal() {
    if (selectedMethods.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 phương thức để thay đổi trạng thái');
        return;
    }
    
    const bulkStatusCount = document.getElementById('bulkStatusCount');
    const bulkStatusCountBtn = document.getElementById('bulkStatusCountBtn');
    if (bulkStatusCount) bulkStatusCount.textContent = selectedMethods.size;
    if (bulkStatusCountBtn) bulkStatusCountBtn.textContent = selectedMethods.size;
    
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedMethods.forEach(methodId => {
            const methodRow = document.querySelector(`.method-checkbox[value="${methodId}"]`);
            if (methodRow) {
                const row = methodRow.closest('tr');
                const methodNameElement = row.querySelector('td:nth-child(3) strong');
                const statusElement = row.querySelector('.status-toggle');
                const methodName = methodNameElement ? methodNameElement.textContent.trim() : 'Unknown';
                const currentStatus = statusElement ? statusElement.textContent.includes('Hoạt động') : true;
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="fas fa-shipping-fast me-2"></i>${methodName}
                        </div>
                        <span class="badge ${currentStatus ? 'bg-success' : 'bg-danger'}">
                            ${currentStatus ? 'Đang hoạt động' : 'Ngưng hoạt động'}
                        </span>
                    </div>
                `;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkUpdateMethodStatus;
    }
    
    const bulkStatusModalElement = document.getElementById('bulkStatusModal');
    if (bulkStatusModalElement) {
        const bulkStatusModal = new bootstrap.Modal(bulkStatusModalElement);
        bulkStatusModal.show();
    }
}

function bulkUpdateMethodStatus() {
    const statusRadio = document.querySelector('input[name="bulkStatusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const methodIds = Array.from(selectedMethods);
    
    const bulkStatusModalElement = document.getElementById('bulkStatusModal');
    const bulkStatusModal = bootstrap.Modal.getInstance(bulkStatusModalElement);
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    let successCount = 0;
    let errorCount = 0;
    const errors = [];
    
    const updatePromises = methodIds.map(methodId => {
        return fetch(`/admin/shipping-methods/toggle-status/${methodId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.ok) {
                successCount++;
            } else {
                errorCount++;
                errors.push(`Lỗi khi cập nhật phương thức ID ${methodId}`);
            }
        })
        .catch(error => {
            errorCount++;
            errors.push(`Lỗi khi cập nhật phương thức ID ${methodId}: ${error.message}`);
        });
    });
    
    Promise.all(updatePromises).then(() => {
        if (successCount > 0) {
            showToast('success', 'Thành công', `Đã cập nhật trạng thái cho ${successCount} phương thức vận chuyển`);
        }
        
        if (errorCount > 0) {
            errors.forEach(error => {
                showToast('error', 'Lỗi cập nhật', error);
            });
        }
        
        setTimeout(() => {
            location.reload();
        }, 1500);
    });
}

// ============= MODAL CLEANUP =============
function setupModalCleanup() {
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('hidden.bs.modal', function() {
            currentMethodId = null;
            currentMethodName = null;
            
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-trash me-1"></i>Xóa';
                confirmBtn.disabled = false;
            }
        });
    }
    
    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('hidden.bs.modal', function() {
            currentMethodId = null;
            currentMethodName = null;
        });
    }
    
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('hidden.bs.modal', function() {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('padding-right');
            document.body.style.removeProperty('overflow');
        });
    });
}