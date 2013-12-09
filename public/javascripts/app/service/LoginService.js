define(['angular'], function(angular) {

	angular.module('LoginService', ['ngResource']).factory('LoginService', function($resource) {
	    var url = decodeURIComponent(javascriptRoutes.controllers.Application.authenticate(':mail', ':password').absoluteURL());
	    url = url.replace(":9000", "\\:9000");
	
	    return $resource(url, {});
	});

});
