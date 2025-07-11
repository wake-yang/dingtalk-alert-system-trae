class PaginationComponent {
    constructor(options) {
        this.pageSize = options.pageSize || 20;
        this.paginationElementId = options.paginationElementId || 'pagination';
        this.loadDataCallback = options.loadDataCallback;
        this.currentPage = 1;
        this.totalPages = 1;
        this.totalRecords = 0;
    }

    renderPagination(current, total, totalRecords) {
        this.currentPage = current;
        this.totalPages = total;
        this.totalRecords = totalRecords;
        
        const pagination = document.getElementById(this.paginationElementId);
        if (!pagination) return;
        
        pagination.innerHTML = '';
    
        if (totalRecords === 0) {
            pagination.innerHTML = `<li class="page-item disabled"><span class="page-link">共 0 条记录</span></li>`;
            return;
        }
    
        // 确保至少有1页
        const totalPagesDisplay = Math.max(1, total);
        
        let html = `<li class="page-item disabled"><span class="page-link">共 ${totalRecords} 条记录</span></li>`;
        html = `<li class="page-item disabled"><span class="page-link">第 ${current}/${totalPagesDisplay} 页</span></li>` + html;
    
        const createPageLink = (page, text) => {
            return `<li class="page-item"><a class="page-link" href="#" onclick="event.preventDefault(); window.paginationInstance.changePage(${page});">${text}</a></li>`;
        };
    
        const activeLink = (page) => {
            return `<li class="page-item active"><span class="page-link">${page}</span></li>`;
        };
    
        const disabledLink = (text) => {
            return `<li class="page-item disabled"><span class="page-link">${text}</span></li>`;
        };
    
        // 上一页 - 始终显示，但在第一页时禁用
        if (current > 1) {
            html += createPageLink(current - 1, '上一页');
        } else {
            html += disabledLink('上一页');
        }
    
        // 页码 - 始终显示当前页
        if (totalPagesDisplay === 1) {
            // 只有一页时，显示当前页为激活状态
            html += activeLink(1);
        } else {
            // 多页时，使用原有逻辑
            const startPage = Math.max(1, current - 2);
            const endPage = Math.min(totalPagesDisplay, current + 2);
    
            if (startPage > 1) {
                html += createPageLink(1, '1');
                if (startPage > 2) html += disabledLink('...');
            }
    
            for (let i = startPage; i <= endPage; i++) {
                if (i === current) {
                    html += activeLink(i);
                } else {
                    html += createPageLink(i, i.toString());
                }
            }
    
            if (endPage < totalPagesDisplay) {
                if (endPage < totalPagesDisplay - 1) html += disabledLink('...');
                html += createPageLink(totalPagesDisplay, totalPagesDisplay.toString());
            }
        }
    
        // 下一页 - 始终显示，但在最后一页时禁用
        if (current < totalPagesDisplay) {
            html += createPageLink(current + 1, '下一页');
        } else {
            html += disabledLink('下一页');
        }
    
        pagination.innerHTML = html;
    }

    changePage(page) {
        if (page < 1 || page > this.totalPages || page === this.currentPage) {
            return;
        }
        this.currentPage = page;
        if (this.loadDataCallback) {
            this.loadDataCallback(page);
        }
    }

    // 获取当前页码
    getCurrentPage() {
        return this.currentPage;
    }

    // 获取总页数
    getTotalPages() {
        return this.totalPages;
    }

    // 获取总记录数
    getTotalRecords() {
        return this.totalRecords;
    }
}

// 导出到全局作用域
window.PaginationComponent = PaginationComponent;