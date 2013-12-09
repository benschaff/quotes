define(['angular'], function(angular) {

	angular.module('DemoService', ['ngResource']).factory('DemoService', function($resource) {
	    var url = stockJavascriptRoutes.controllers.StockController.demo().absoluteURL();
	    url = url.replace(":9000", "\\:9000");
	
	    return $resource(url, {});
	});

});
