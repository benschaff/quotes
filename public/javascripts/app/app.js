define('angular', [ 'webjars!angular.js', 'webjars!angular-resource.js' ], function() {
    return angular;
});

requirejs.config({
	paths: {
        'event-source': '../vendors/event-source/jquery.eventsource'
    },
	shim : {
		bootstrap : [ 'webjars!jquery.js' ],
        'angular-route': [ 'angular' ],
        'angular-animate': [ 'angular' ],
        angular: [ 'webjars!highcharts.js', 'webjars!md5.js' ],
        highcharts: [ 'webjars!jquery.js' ],
        md5: [ 'webjars!core.js' ],
        'event-source': [ 'webjars!jquery.js' ]
	},
    priority: [ 'webjars!jquery.js', 'webjars!highcharts.js', 'webjars!core.js', 'webjars!md5.js', 'angular' ]
});

function installFunctions($rootScope, $location, LoginService) {
    $rootScope.messages = [];
    $rootScope.user = null;
    $rootScope.shared = {
        mail: null,
        password: null,
        authenticate: function(silent) {
            silent = silent || false;

            LoginService.get(
                {
                    mail: $rootScope.shared.mail,
                    password: CryptoJS.MD5($rootScope.shared.password)
                },
                function(user) {
                    $rootScope.user = user;

                    $location.path("/portfolio");

                    $rootScope.$root.$eval();
                },
                function(error) {
                    if (silent) {
                        console.log(error);

                        $rootScope.setMessage({ type: 'error', text: 'Invalid username or password.' });
                    }
                }
            );
        },
        signOff: function() {
            LoginService.delete();

            $rootScope.user = null;

            $rootScope.$root.$eval();

            $location.path("/");
        },
        navigate: function(path) {
            $location.path(path);
        }
    };

    $rootScope.dismissMessage = function() {
        $rootScope.messages = [];

        $rootScope.$root.$eval();
    };

    $rootScope.setMessage = function(message) {
        $rootScope.messages = [ message ];

        $rootScope.$root.$eval();
    };

}

require(
    [
        'angular',
        './controller/MainController',
        './controller/SymbolsController',
        './controller/PortfolioController',
        './controller/SignUpController',
        './service/DemoService',
        './service/ChartingService',
        './service/SymbolsService',
        './service/LoginService',
        './service/QuotesService',
        './service/SignUpService',
        './service/EventSourceService',
        'webjars!jquery.js',
        'webjars!bootstrap.min.js',
        'webjars!angular-route.js',
        'webjars!angular-animate.js',
        'webjars!highcharts.js',
        'webjars!core.js',
        'webjars!md5.js',
        'event-source'
    ],
	function (angular, MainController, SymbolsController, PortfolioController, SignUpController) {
		angular.module('app',
	            [
	                'ngResource',
                    'ngRoute',
                    'ngAnimate',
	                'DemoService',
                    'ChartingService',
                    'SymbolsService',
                    'FollowSymbolService',
                    'QuotesService',
                    'LoginService',
                    'SignUpService',
                    'EventSourceService'
	            ]
		).config(['$routeProvider', function($routeProvider) {
			$routeProvider.when('/', { templateUrl: 'assets/javascripts/app/view/main.html', controller: MainController, resolve: MainController.resolve })
                          .when('/?mail=:mail', { templateUrl: 'assets/javascripts/app/view/main.html', controller: MainController, resolve: MainController.resolve })
                          .when('/symbols', { templateUrl: 'assets/javascripts/app/view/symbols.html', controller: SymbolsController })
                          .when('/portfolio', { templateUrl: 'assets/javascripts/app/view/portfolio.html', controller: PortfolioController, resolve: PortfolioController.resolve })
                          .when('/signup', { templateUrl: 'assets/javascripts/app/view/signup.html', controller: SignUpController })
                          .when('/signup/:mail', { templateUrl: 'assets/javascripts/app/view/signup.html', controller: SignUpController })
						  .otherwise({ redirectTo: '/' });
		}]).run(function($rootScope, $location, LoginService) {
                installFunctions($rootScope, $location, LoginService);

                // register listener to watch route changes
                $rootScope.$on("$routeChangeStart", function(event, next) {
                    if (!$rootScope.user) {
                        if (next.$$route && next.$$route.templateUrl && next.$$route.templateUrl.indexOf("portfolio.html") != -1) {
                            $location.path("/");
                        }
                    }
                });
        // From https://github.com/rootux/angular-highcharts-directive/blob/master/src/directives/highchart.js
        }).directive('chart', function () {
            return {
                restrict: 'E',
                template: '<div></div>',
                scope: {
                    chartData: "=value"
                },
                transclude: true,
                replace: true,

                link: function (scope, element, attrs) {
                    var chartsDefaults = {
                        chart: {
                            renderTo: element[0],
                            type: attrs.type || null,
                            height: attrs.height || null,
                            width: attrs.width || null
                        }
                    };

                    //Update when charts data changes
                    scope.$watch(function() { return scope.chartData; }, function(value) {
                        if(!value) return;
                        // We need deep copy in order to NOT override original chart object.
                        // This allows us to override chart data member and still the keep
                        // our original renderTo will be the same
                        var deepCopy = true;
                        var newSettings = {};
                        $.extend(deepCopy, newSettings, chartsDefaults, scope.chartData);
                        new Highcharts.Chart(newSettings);
                    });
                }
            };
        });
		
		angular.bootstrap(document, ['app']);
	}
);