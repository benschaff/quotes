define(['angular'], function(angular) {

	angular.module('EventSourceService', ['ngResource']).factory('EventSourceService', function() {
        var eventSourcingService = {
            handlers: {},
            open: function(url, source, openHandler, messageHandler) {
                if (eventSourcingService.handlers[source] && eventSourcingService.handlers[source].length) {
                    eventSourcingService.handlers[source].push(messageHandler);
                } else {
                    eventSourcingService.handlers[source] = [];
                    eventSourcingService.handlers[source].push(messageHandler);

                    $.eventsource({
                        label: source,
                        url: url,
                        dataType: "json",
                        open: function(data) {
                            console.log(data);

                            openHandler(data);
                        },
                        message: function(data) {
                            console.log(data);

                            $.each(eventSourcingService.handlers[source], function(index, handler) {
                                handler(data);
                            });
                        }
                    });
                }
            },
            close: function(source) {
                $.eventsource("close", source);
            }
        };

        return eventSourcingService;
	});

});
