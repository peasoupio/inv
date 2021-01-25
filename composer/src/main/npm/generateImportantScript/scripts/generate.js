var fs = require('fs')
var path = require("path")

console.log("Working in: " + __dirname)

var importantFile = path.join(__dirname, "../important")
if (!fs.existsSync(importantFile))
    throw "important file does not"

fs.readFile(importantFile, 'utf8', function (err,data) {
    if (err) {
        return console.log(err);
    }
    console.log(data);
})