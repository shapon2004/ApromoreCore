
export function require(filePath) {
    const fs = require('fs');
    const fileContents = fs.readFileSync('./myFile').toString();
    return fileContents;
}
