// Toggle functions for expanding/collapsing details

function toggleDetailButton(button, expand) {
    const value = JSON.parse(button.dataset.value);
    const key = button.dataset.key;
    const depth = parseInt(button.dataset.depth);

    if (!button.classList.contains('expanded')) {
        const nestedTable = createTable(value, key, depth + 1);
        nestedTable.style.display = 'none';
        button.tableDiv.appendChild(nestedTable);
        button.classList.add('expanded');
        button.nestedTable = nestedTable;
    }
    const isVisible = expand !== undefined ? expand : button.nestedTable.style.display === 'none';
    button.nestedTable.style.display = isVisible ? 'block' : 'none';
    button.textContent = isVisible ? "Collapse Details" : "Expand Details";
}

function toggleAllDetails(expand) {
    const toggleButtons = Array.from(document.querySelectorAll('.toggle-button')).filter(button => button.dataset.value);
    toggleButtons.forEach(button => {
        try {
            const value = JSON.parse(button.dataset.value);
            if (value) {
                toggleDetailButton(button, expand);
            }
        } catch (e) {
            console.warn("Skipping button due to invalid JSON in dataset.value:", button);
        }
    });
    document.getElementById('toggleAllButton').textContent = expand ? "Collapse All Details" : "Expand All Details";
}
