define(function() {
	
	SymbolsController = function($rootScope, $scope, SymbolsService) {
        $rootScope.action = 'symbols';

        $scope.symbols = [];
        $scope.query = null;

        $scope.$watch('query', function(value) {
            if (!value) {
                $scope.query = null;
                $scope.symbols = [];
            }
        });

        $scope.search = function(query) {
            SymbolsService.query(
                { query: query },
                function (symbols) {
                    if (symbols) {
                        $scope.symbols = symbols;
                    } else {
                        $scope.symbols = [];
                    }
                },
                function (error) {
                    console.log(error);

                    $rootScope.setMessage({ type: 'error', text: 'An error occured. Please try again later' });
                }
            );
        };
	};
	
	return SymbolsController;

});
