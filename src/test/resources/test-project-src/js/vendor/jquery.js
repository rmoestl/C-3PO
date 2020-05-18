(function() {
  document.onreadystatechange = function () {
    if (document.readyState === "complete") {
      console.log("This ain't jQuery, but we pretend it to be :-).");
      document.jQuery = {};
    }
  }
})();
