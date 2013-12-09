define(['angular'], function(angular) {

	angular.module('QuotesService', ['ngResource']).factory('QuotesService', function($resource) {
	    var url = decodeURIComponent(stockJavascriptRoutes.controllers.StockController.quotes(':symbols').absoluteURL());
	    url = url.replace(":9000", "\\:9000");
	
	    return $resource(url, {});
	});

});
