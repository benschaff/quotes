define(function() {
	
	MainController = function($rootScope, $scope, $filter, $routeParams, demo, Last30DaysService) {
        $rootScope.action = 'home';

        if ($routeParams.mail) {
            $rootScope.shared.mail = $routeParams.mail;

            $rootScope.shared.authenticate(true);

            return;
        }

        $scope.changeSymbol = function(symbol) {
            Last30DaysService.get(
                { symbol: symbol.symbol },
                function(chartData) {
                    var categories = [];
                    $.each(chartData.Dates, function(index, date) {
                        categories.push($filter('date')(date, 'dd/MM'));
                    });

                    $scope.currentSymbolChartData = {
                        "title": {
                            "text": "Price history of " + symbol.name
                        },
                        "subtitle": {
                            "text": "30 days period"
                        },
                        "xAxis": {
                            "categories": categories
                        },
                        "tooltip": {},
                        "plotOptions": {
                            "area": {
                                "pointStart": chartData.Elements[0].DataSeries.close.values[0]
                            }
                        },
                        "series": [
                            {
                                "name": "Close price",
                                "data": chartData.Elements[0].DataSeries.close.values
                            }
                        ]
                    };

                    $scope.currentSymbol = symbol;
                },
                function(error) {
                    console.log(error);

                    $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });
                }
            );
        };

        if (demo.symbols) $scope.changeSymbol(demo.symbols[0]);
        $scope.demo = demo;
	};

    MainController.resolve = {
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
    };
	
	return MainController;

});
