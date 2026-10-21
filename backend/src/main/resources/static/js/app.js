// Musomi Manager Student Portal Helper Scripts
document.addEventListener('DOMContentLoaded', () => {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
    
    // Check for prefers-reduced-motion
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) {
        document.documentElement.style.setProperty('--transition-duration', '0ms');
    }
});

// Re-initialize Lucide icons after any HTMX DOM swap
document.body.addEventListener('htmx:afterSwap', () => {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});

// HTMX Response & Error handling
document.body.addEventListener('htmx:beforeSwap', function(evt) {
    if (evt.detail.xhr.status === 400 || evt.detail.xhr.status === 422) {
        evt.detail.shouldSwap = true;
        evt.detail.isError = false;
    }
});

document.body.addEventListener('htmx:responseError', function(evt) {
    if (evt.detail.xhr.status === 401 || (evt.detail.xhr.responseURL && evt.detail.xhr.responseURL.endsWith('/login'))) {
        window.location.href = '/login';
    } else if (evt.detail.xhr.status === 403) {
        window.location.href = '/access-denied';
    } else if (evt.detail.xhr.status >= 500) {
        window.location.href = '/500';
    }
});

// Client-side Toast notification helper
window.showToast = function(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `p-4 rounded-xl shadow-lg border flex items-start gap-3 transition-all duration-300 transform translate-y-2 opacity-0
        ${type === 'success' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 
          type === 'error' ? 'bg-red-50 border-red-200 text-red-800' : 
          'bg-indigo-50 border-indigo-200 text-indigo-800'}`;
          
    let iconName = 'info';
    let iconColor = 'text-indigo-600';
    if (type === 'success') {
        iconName = 'check-circle';
        iconColor = 'text-emerald-600';
    } else if (type === 'error') {
        iconName = 'alert-circle';
        iconColor = 'text-red-600';
    }
    
    toast.innerHTML = `
        <i data-lucide="${iconName}" class="w-5 h-5 shrink-0 mt-0.5 ${iconColor}"></i>
        <div class="flex-grow text-sm font-medium leading-relaxed">${message}</div>
        <button type="button" class="shrink-0 rounded-md p-1 hover:bg-black/5 focus:outline-none" onclick="this.parentElement.remove()">
            <span class="sr-only">Close</span>
            <i data-lucide="x" class="w-4 h-4 text-slate-500"></i>
        </button>
    `;
    
    container.appendChild(toast);
    if (typeof lucide !== 'undefined') {
        lucide.createIcons({ root: toast });
    }
    
    requestAnimationFrame(() => {
        toast.classList.remove('translate-y-2', 'opacity-0');
        toast.classList.add('translate-y-0', 'opacity-100');
    });
    
    setTimeout(() => {
        toast.classList.remove('translate-y-0', 'opacity-100');
        toast.classList.add('translate-y-2', 'opacity-0');
        setTimeout(() => toast.remove(), 300);
    }, 4500);
};

// Listen for custom trigger events from HTMX headers
document.body.addEventListener('show-toast', function(evt) {
    if (evt.detail && evt.detail.value) {
        showToast(evt.detail.value.message, evt.detail.value.type);
    }
});
