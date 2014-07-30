define(['angular'], function(angular) {

    angular.module('app.controllers').controller('SignUpController', [ '$rootScope', '$scope', '$location', '$routeParams', 'SignUpService', function($rootScope, $scope, $location, $routeParams, SignUpService) {
        $scope.mail = $routeParams.mail;
        $scope.password = null;

        $scope.create = function() {
            if (!$scope.mail || !$scope.password) return false;

            SignUpService.get(
                { mail: $scope.mail, password: CryptoJS.MD5($scope.password) },
                function(user) {
                    $scope.mail = null;
                    $scope.password = null;

                    $rootScope.user = user;

                    $scope.$root.$eval();

                    $location.path("portfolio");
                },
                function (error) {
                    console.log(error);

                    if (error.status == 406) $rootScope.setMessage({ type: 'error', text: 'Mail is already used.' });
                    else $rootScope.setMessage({ type: 'error', text: 'Error while trying to create your account.' });
                }
            );

            return false;
        };
    }]);

});
