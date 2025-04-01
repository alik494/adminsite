$(document).ready(function() {
    $(".searchButton").click(function(e) {
        e.preventDefault();

        var rootPathValue = $("input[name='rootPath']").val();
        var propertyNameValue = $("input[name='propertyName']").val();
        var propertyValueValue = $("input[name='propertyValue']").val();

        $.ajax({
            url: '/bin/searchComponents',
            type: 'GET',
            data: {
                rootPath: rootPathValue,
                propertyName: propertyNameValue,
                propertyValue: propertyValueValue
            },
            dataType: 'json',
            success: function(response) {
                var table =  $(".coral-Table");

                 if (table) {

                    $(".coral-Table-cell").remove();

                    if (table[0].children[0].innerHTML.length <= 800) {
                        var headerRow = new Coral.Table.Row();

                        var nameHeader = new Coral.Table.HeaderCell();
                        nameHeader.content.innerText = "Name";
                        headerRow.appendChild(nameHeader);

                        var descHeader = new Coral.Table.HeaderCell();
                        descHeader.content.innerText = "Description";
                        headerRow.appendChild(descHeader);

                        var pathHeader = new Coral.Table.HeaderCell();
                        pathHeader.content.innerText = "Path";
                        headerRow.appendChild(pathHeader);

                        var typeHeader = new Coral.Table.HeaderCell();
                        typeHeader.content.innerText = "ResSuperType";
                        headerRow.appendChild(typeHeader);

                        table.append(headerRow);
                     }

                    response.forEach(function(item) {
                       var row = new Coral.Table.Row();

                       var nameCell = new Coral.Table.Cell();
                       nameCell.content.innerText = item.name;
                       row.appendChild(nameCell);

                       var descCell = new Coral.Table.Cell();
                       descCell.content.innerText = item.description;
                       row.appendChild(descCell);

                       var pathCell = new Coral.Table.Cell();
                       pathCell.content.innerText = item.path;
                       row.appendChild(pathCell);

                       var typeCell = new Coral.Table.Cell();
                       typeCell.content.innerText = item.restype;
                       row.appendChild(typeCell);

                       table.append(row);
                    });
                 } else {
                     console.error("Coral table element is not found on the page.");
                 }
            },
            error: function(xhr, status, error) {
                console.error("Error: " + status + " " + error);
            }
        });
    });
});