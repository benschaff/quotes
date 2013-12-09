define(['angular'], function(angular) {

	angular.module('SymbolsService', ['ngResource']).factory('SymbolsService', function($resource) {
	    var url = decodeURIComponent(stockJavascriptRoutes.controllers.StockController.symbols(':query').absoluteURL());
	    url = url.replace(":9000", "\\:9000");
	
	    return $resource(url, {});
	});

    angular.module('FollowSymbolService', ['ngResource']).factory('FollowSymbolService', function($resource) {
        var url = decodeURIComponent(stockJavascriptRoutes.controllers.StockController.follow(':symbol').absoluteURL());
        url = url.replace(":9000", "\\:9000");

        return $resource(url, {});
    });

});
