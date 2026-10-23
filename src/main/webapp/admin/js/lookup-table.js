document.addEventListener('DOMContentLoaded',()=>{
    const search=document.getElementById('lookup-search'),table=document.getElementById('datatablesSimple');
    if(!search || !table) return;
    search.addEventListener('input',()=>{
        if(window.jQuery?.fn?.dataTable?.isDataTable(table)) {window.jQuery(table).DataTable().search(search.value).draw();return;}
        const term=search.value.trim().toLocaleLowerCase('vi');
        for(const row of table.querySelectorAll('tbody tr')) row.hidden=!row.textContent.toLocaleLowerCase('vi').includes(term);
    });
});
