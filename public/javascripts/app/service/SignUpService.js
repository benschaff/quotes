define(['angular'], function(angular) {

	angular.module('SignUpService', ['ngResource']).factory('SignUpService', function($resource) {
	    var url = decodeURIComponent(javascriptRoutes.controllers.Application.signUp(':mail', ':password').absoluteURL());
	    url = url.replace(":9000", "\\:9000");
	
	    return $resource(url, {});
	});

});
