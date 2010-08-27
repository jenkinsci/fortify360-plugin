// to refresh the F360 project list
function refreshF360Button(url,paramList,button) {    
    button = button._button;
    
    var parameters = {};
    paramList.split(',').each(function(name) {
        var p = findPreviousFormItem(button,name);
        if(p!=null) {
            if(p.type=="checkbox")  parameters[name] = p.checked;
            else                    parameters[name] = p.value;
        }
    });
    
    var spinner = Element.up(button,"DIV").nextSibling;
    spinner.style.display="block";
    
    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function(rsp) {
            spinner.style.display="none";
            var respText = rsp.responseText;
            var list = eval(respText);
            var select = document.getElementById('f360projId');
            if ( select ) {
          		// remove old options first
          		for(var i=select.options.length-1; i>=0; i--) select.remove(i);
          		// and then add new values
            	for(var i=0; i<list.length; i++) {
                	var item = list[i];
          	    	select.options.add(new Option(item.name, item.id));
            	}
            }
        }
    });
}        