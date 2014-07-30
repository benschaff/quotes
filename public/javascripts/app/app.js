var webjars = {
    versions: {
        'requirejs': '2.1.10',
        'jquery': '2.1.0',
        'angularjs': '1.2.19',
        'bootstrap': '3.2.0',
        'highcharts': '4.0.1',
        'cryptojs': '3.1.2'
    },
    path: function(webjarid, path) {
        return '/webjars/' + webjarid + '/' + webjars.versions[webjarid] + '/' + path;
    }
};

define('webjars', function () {
    return { load: function (name, req, onload, config) { onload(); } }
});

requirejs.config({
	paths: {
        'jquery': webjars.path("jquery", "jquery"),
        'angular': webjars.path("angularjs", "angular"),
        'angular-route': webjars.path("angularjs", "angular-route"),
        'angular-animate': webjars.path("angularjs", "angular-animate"),
        'angular-resource': webjars.path("angularjs", "angular-resource"),
        'bootstrap': webjars.path("bootstrap", "js/bootstrap"),
        'highcharts': webjars.path("highcharts", "highcharts"),
        'cryptojs-core': webjars.path("cryptojs", "components/core"),
        'cryptojs-md5': webjars.path("cryptojs", "components/md5"),
        'event-source': '../vendors/event-source/jquery.eventsource'
    },
	shim : {
        'jquery': { "exports": "$" },
        'angular': { deps: [ 'jquery' ], "exports": "angular" },
        'angular-route': { deps: [ 'angular' ] },
        'angular-animate': { deps: [ 'angular' ] },
        'angular-resource': { deps: [ 'angular' ] },
        'bootstrap': { deps: [ 'jquery' ], "exports": "bootstrap" },
        'highcharts': { deps: [ 'jquery' ] },
        'cryptojs-core': { "exports": "core" },
        'cryptojs-md5': { deps: [ 'cryptojs-core' ], "exports": "md5" },
        'event-source': [ 'jquery' ]
	}
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

require(['angular'], function(angular) {
    angular.module('app.controllers', []);
});

require(
    [
        'angular',
        'angular-route',
        'angular-animate',
        'angular-resource',
        'jquery',
        'bootstrap',
        'highcharts',
        'cryptojs-core',
        'cryptojs-md5',
        'event-source',
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
        './service/EventSourceService'
    ],
	function (angular) {

        angular.module('app',
            [
                'ngResource',
                'ngRoute',
                'ngAnimate',
                'app.controllers',
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
			$routeProvider.when('/', {
                templateUrl: 'assets/javascripts/app/view/main.html',
                controller: 'MainController',
                resolve: {
                    demo: function(DemoService, $q, $rootScope) {
                        var deferred = $q.defer();

                        DemoService.get(function(demo) {
                            deferred.resolve(demo);
                        }, function(error) {
                            console.log(error);

                            $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });

                            deferred.resolve({});
                        });

                        return deferred.promise;
                    }
                }
            }).when('/?mail=:mail', {
                templateUrl: 'assets/javascripts/app/view/main.html',
                controller: 'MainController',
                resolve: {
                    demo: function(DemoService, $q, $rootScope) {
                        var deferred = $q.defer();

                        DemoService.get(function(demo) {
                            deferred.resolve(demo);
                        }, function(error) {
                            console.log(error);

                            $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });

                            deferred.resolve({});
                        });

                        return deferred.promise;
                    }
                }
            }).when('/symbols', {
                templateUrl: 'assets/javascripts/app/view/symbols.html',
                controller: 'SymbolsController'
            }).when('/portfolio', {
                templateUrl: 'assets/javascripts/app/view/portfolio.html',
                controller: 'PortfolioController',
                resolve: {
                    quotes: function(QuotesService, $q, $rootScope) {
                        if (!$rootScope.user || !$rootScope.user.followedStocks.length) return null;

                        var deferred = $q.defer();

                        var symbolList = $rootScope.user.followedStocks.join(",");
                        QuotesService.query(
                            { symbols: symbolList },
                            function(quotes) {
                                deferred.resolve(quotes);
                            }, function(error) {
                                console.log(error);

                                $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });

                                deferred.resolve({});
                            }
                        );

                        return deferred.promise;
                    }
                }
            }).when('/signup', {
                templateUrl: 'assets/javascripts/app/view/signup.html',
                controller: 'SignUpController'
            }).when('/signup/:mail', {
                templateUrl: 'assets/javascripts/app/view/signup.html',
                controller: 'SignUpController'
            }).otherwise({ redirectTo: '/' });
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