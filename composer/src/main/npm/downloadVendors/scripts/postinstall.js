var fs = require('fs')
var path = require("path")

console.log("Working in: " + __dirname)
console.log("Copying node_modules to composer's vendor folder: " + __dirname)

var vendorsFile = path.join(__dirname, "../vendors.json")
var nodeModulesDir = path.join(__dirname, "../node_modules")
var composerVendorDir = path.join(__dirname, "../../../resources/public/vendor")

// List required vendors subpaths
var vendors = JSON.parse(fs.readFileSync(vendorsFile))

console.log("Vendor(s):")
vendors.forEach(function(vendor) {

    var vendorFullpath = path.join(nodeModulesDir, vendor.src)
    var isDir = fs.lstatSync(vendorFullpath).isDirectory()
    var isFile = !isDir

    console.log("%s: %s(%s)", vendor.name, vendor.src, isDir ? "directory" : "file")

    if (isFile) {
        copyFile(vendorFullpath, vendor.newName)
    } else {
        fs.readdir(vendorFullpath, function(err, files) {
            if (err) throw err

            files.forEach(function(vendorSubfile) {
                copyFile(path.join(vendorFullpath, vendorSubfile))
            })
        })
    }
})


function copyFile(src, newName) {
    var basename = newName || path.basename(src)
    var composerVendorFullpath = path.join(composerVendorDir, basename)

    fs.copyFile(src, composerVendorFullpath, function (err) {
        if (err) throw err;
    })
}


