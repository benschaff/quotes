define(function() {
	
	PortfolioController = function($rootScope, $scope, $filter, quotes, Last30DaysService, FollowSymbolService, EventSourceService) {
        $rootScope.action = 'portfolio';

        $scope.changeSymbol = function(symbol) {
            Last30DaysService.get(
                { symbol: symbol },
                function(chartData) {
                    var categories = [];
                    $.each(chartData.Dates, function(index, date) {
                        categories.push($filter('date')(date, 'dd/MM'));
                    });

                    $scope.symbolChartData = {
                        "title": {
                            "text": "Price history of " + symbol
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

                    $scope.symbol = symbol;
                },
                function(error) {
                    console.log(error);

                    $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });
                }
            );
        };

        if (quotes && quotes.length) $scope.changeSymbol(quotes[0].symbol);

        $scope.quotes = quotes;
        $scope.lastUpdate = new Date();
        $scope.newSymbol = null;

        $scope.register = function() {
            if (!$scope.newSymbol || $rootScope.user.readOnly) return false;

            FollowSymbolService.get(
                { symbol: $scope.newSymbol },
                function(quote) {
                    if (!$scope.quotes) $scope.quotes = [];

                    $scope.quotes.push(quote);

                    $scope.changeSymbol(quote.symbol);

                    $rootScope.user.followedStocks.push(quote.symbol);

                    $scope.newSymbol = null;

                    $scope.$root.$eval();
                },
                function(error) {
                    console.log(error);

                    $rootScope.setMessage(
                        { type: 'error', text: 'Something went wrong when trying to add symbol ' +  $scope.newSymbol}
                    );
                }
            );

            return false;
        };

        $scope.unregister = function($event, symbol) {
            $event.preventDefault();

            if ($rootScope.user.readOnly) return false;

            FollowSymbolService.delete(
                { symbol: symbol },
                function(symbol) {
                    $.each($scope.quotes, function(index, quote) {
                        if (quote.symbol == symbol.symbol) {
                            $scope.quotes.splice(index, 1);

                            if ($scope.symbol == symbol.symbol) {
                                $scope.symbolChartData = null;
                                $scope.symbol = null;

                                if ($scope.quotes.length > 0) {
                                    $scope.changeSymbol($scope.quotes[0].symbol);
                                }
                            }

                            return false;
                        }

                        return true;
                    });

                    $.each($rootScope.user.followedStocks, function(index, userSymbol) {
                        if (userSymbol == symbol.symbol) {
                            $rootScope.user.followedStocks.splice(index, 1);

                            return false;
                        }

                        return true;
                    });

                    $scope.$root.$eval();
                },
                function(error) {
                    console.log(error);

                    $rootScope.setMessage(
                        { type: 'error', text: 'Something went wrong when trying to remove symbol ' +  symbol}
                    );
                }
            );

            return false;
        };

        var url = stockJavascriptRoutes.controllers.StockController.quoteSource().absoluteURL();
        EventSourceService.open(
            url, "quote",
            function(data) {
            },
            function(quote) {
                if (!$scope.quotes) $scope.quotes = [];

                $.each($scope.quotes, function(index, oldQuote) {
                    if (quote.symbol == oldQuote.symbol) {
                        $scope.quotes[index] = quote;

                        return false;
                    }

                    return true;
                });

                if ($scope.symbol == quote.symbol) $scope.changeSymbol(quote.symbol);

                $scope.lastUpdate = new Date();

                $scope.$root.$eval();
                $scope.$apply();
            }
        );
	};

    PortfolioController.resolve = {
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
    };
	
	return PortfolioController;

});
