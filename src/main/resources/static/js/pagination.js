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

        let html = `<li class="page-item disabled"><span class="page-link">共 ${totalRecords} 条记录</span></li>`;

        if (total <= 1) {
            pagination.innerHTML = html;
            return;
        }

        html = `<li class="page-item disabled"><span class="page-link">第 ${current}/${total} 页</span></li>` + html;

        const createPageLink = (page, text) => {
            return `<li class="page-item"><a class="page-link" href="#" onclick="event.preventDefault(); window.paginationInstance.changePage(${page});">${text}</a></li>`;
        };

        const activeLink = (page) => {
            return `<li class="page-item active"><span class="page-link">${page}</span></li>`;
        };

        const disabledLink = (text) => {
            return `<li class="page-item disabled"><span class="page-link">${text}</span></li>`;
        };

        // 上一页
        if (current > 1) {
            html += createPageLink(current - 1, '上一页');
        } else {
            html += disabledLink('上一页');
        }

        // 页码
        const startPage = Math.max(1, current - 2);
        const endPage = Math.min(total, current + 2);

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

        if (endPage < total) {
            if (endPage < total - 1) html += disabledLink('...');
            html += createPageLink(total, total.toString());
        }

        // 下一页
        if (current < total) {
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