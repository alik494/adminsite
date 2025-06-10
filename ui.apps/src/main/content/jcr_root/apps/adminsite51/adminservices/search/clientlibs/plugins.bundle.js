$(document).ready(function() {
    $(".searchButton").click(function(e) {
        e.preventDefault();

        let rootPathValue = $("input[name='rootPath']").val();
        let propertyNameValue = $("input[name='propertyName']").val();
        let propertyValueValue = $("input[name='propertyValue']").val();

        $.ajax({
            url: "/bin/searchComponents",
            type: "GET",
            data: {
                rootPath: rootPathValue,
                propertyName: propertyNameValue,
                propertyValue: propertyValueValue
            },
            dataType: "json",
            success: function(response) {
                let table =  $(".coral-Table");

                 if (table) {

                    $(".coral-Table-cell").remove();

                    if (table[0].children[0].innerHTML.length <= 800) {
                        let headerRow = new Coral.Table.Row();

                        let nameHeader = new Coral.Table.HeaderCell();
                        nameHeader.content.innerText = "Name";
                        headerRow.appendChild(nameHeader);

                        let descHeader = new Coral.Table.HeaderCell();
                        descHeader.content.innerText = "Description";
                        headerRow.appendChild(descHeader);

                        let pathHeader = new Coral.Table.HeaderCell();
                        pathHeader.content.innerText = "Path";
                        headerRow.appendChild(pathHeader);

                        let typeHeader = new Coral.Table.HeaderCell();
                        typeHeader.content.innerText = "ResSuperType";
                        headerRow.appendChild(typeHeader);

                        table.append(headerRow);
                     }

                    response.forEach(function(item) {
                       let row = new Coral.Table.Row();

                       let nameCell = new Coral.Table.Cell();
                       nameCell.content.innerText = item.name;
                       row.appendChild(nameCell);

                       let descCell = new Coral.Table.Cell();
                       descCell.content.innerText = item.description;
                       row.appendChild(descCell);

                       let pathCell = new Coral.Table.Cell();
                       pathCell.content.innerText = item.path;
                       row.appendChild(pathCell);

                       let typeCell = new Coral.Table.Cell();
                       typeCell.content.innerText = item.restype;
                       row.appendChild(typeCell);

                       table.append(row);
                    });
                    addExportCSVButton(response);
                 } else {
                     console.error("Coral table element is not found on the page.");
                 }
            },
            error: function(xhr, status, error) {
                console.error("Error: " + status + " " + error);
            }
        });
    });

    function addExportCSVButton(data) {
        $(".exportCSVButton").click(function() {
            generateAndDownloadCSV(data);
        });
    }

    function generateAndDownloadCSV(jsonData) {
        if (!jsonData || jsonData.length === 0) {
            alert("No data available to export");
            return;
        }

        const headers = Object.keys(jsonData[0]).join(",") + "\n";

        const rows = jsonData.map(obj => {
            return Object.values(obj).join(",");
        }).join("\n");

        const csvContent = headers + rows;

        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", "data.csv");
        link.style.visibility = "hidden";

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
});