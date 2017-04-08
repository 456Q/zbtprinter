# zbtprinter
A Cordova/Phonegap driver for Zebra bluetooth printers

This is a fork of https://github.com/michael79bxl/zbtprinter with link-os support added.

- Zebra SDK Updated to v2.11.2800
- Tested with Zebra ZQ520 (ZQ500 Series)

It also includes a function for image printing.


##Usage
You can find Zebra printer using:

```
cordova.plugins.zbtprinter.find(function(result) { 
        if(typeof result == 'string') {
          alert(mac); 
        } else {
          alert(result.address + ', ' + result.friendlyName);
        }
    }, function(fail) { 
        alert(fail); 
    }
);
```

You can send data in ZPL Zebra Programing Language:

```
var strData = "! U1 setvar "device.languages" "line_print"\r\nTEXT ***Print test***\r\nPRINT\r\n";

cordova.plugins.zbtprinter.print("AC:3F:A4:1D:7A:5C", strData,
    function(success) { 
        alert("Print ok"); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

Or send base64 encoded image to the printer:

```
var imgData = "data:image/png;base64,xxxxyyyyzzzz=";
imgData = imgData.replace("data:image/png;base64,", "");

cordova.plugins.zbtprinter.image("AC:3F:A4:1D:7A:5C", "Test Name", imgData,
             function (success) {
                 alert('Image Print ok');
             },
             function (fail) {
                 alert(fail);
             }
         );
```

Or use the batch mode to combine data und image:

```

var batch = [];
 
var strData = "! U1 setvar "device.languages" "line_print"\r\nTEXT ***Print test***\r\nPRINT\r\n";
var job = {
            typ: "data",
            string: strData
        };

batch.push(job);
		
var imgData = "data:image/png;base64,xxxxyyyyzzzz=";
imgData = imgData.replace("data:image/png;base64,", "");

var job = {
	typ: "image",
	string: imgData,
	title: "Test Title"
};

batch.push(job);
		
cordova.plugins.zbtprinter.batch("AC:3F:A4:1D:7A:5C", batch,
    function(success) { 
        alert("Print ok"); 
		}, function(fail) { 
			alert(fail); 
		}
	);
```

##Install
###Cordova

```
cordova plugin add https://github.com/bstmedia/zbtprinter.git
```


##ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
