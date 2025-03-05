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

                    response.forEach(function(item) {
                        var row = new Coral.Table.Row();

                        var nameCell = new Coral.Table.Cell();
                        nameCell.content.innerText = item.name;
                        row.appendChild(nameCell);

                        var descCell = new Coral.Table.Cell();
                        descCell.content.innerText = item.description;
                        row.appendChild(descCell);

                        var typeCell = new Coral.Table.Cell();
                        typeCell.content.innerText = item.resourceType;
                        row.appendChild(typeCell);

                        table.add(row);
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