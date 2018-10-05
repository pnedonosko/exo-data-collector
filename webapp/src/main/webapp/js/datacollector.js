(function($) {
	"use strict";
	
	$(document).ready(function() {
	 
	 	// Add default eye icons
		$( ".actionBar" ).find( ".statusAction" ).prepend( "<li class='eye eye-default'></li>");

		
		// Listener for changing icon state
		addEyeOnClickListener($(".eye"));

		// Observe changes in the stream to add icon to new activities
		var observer = new MutationObserver(function(mutations) {
		  mutations.forEach(function(mutation) {
		  	
		    if(mutation.addedNodes.length > 3){
		    	$(mutation.target).find(".statusAction").prepend("<li class='eye eye-default'></li>");
		    	addEyeOnClickListener($(mutation.target).find(".eye"));
		    }
		  });    
		});
		 
		observer.observe($("#UIUserActivitiesDisplay").get(0), {childList: true, subtree: true});

		});

	// Adds on click listener to the elements
	function addEyeOnClickListener(elements) {
		$(elements).click(function() {
			if($(this).attr("onClick") == undefined){
				var link = $(this).parents(".boxContainer").find('.heading > .actLink > a')[0];
				var activityId = $(link).attr("href").substring($(link).attr("href").indexOf('=') + 1);
				
				if($(this).hasClass("eye-default")){
					console.log("Action: relevant | ID: " + activityId);
					sendRelevance(activityId, true);
					$(this).removeClass("eye-default");
					$(this).toggleClass('eye-relevant');
				}
				else if($(this).hasClass("eye-relevant")){
					
					console.log("Action: irrelevant | ID: " + activityId);
					sendRelevance(activityId, false);
					$(this).removeClass("eye-relevant");
					$(this).toggleClass('eye-irrelevant');
				}
				else{
					console.log("Action: relevant | ID: " + activityId);
					sendRelevance(activityId, true);
					$(this).removeClass("eye-irrelevant");
					$(this).toggleClass('eye-relevant');
				}
		}
		});
	}
	
	// Finds out page base url
	var pageBaseUrl = function(theLocation) {
		if (!theLocation) {
			theLocation = window.location;
		}

		var theHostName = theLocation.hostname;
		var theQueryString = theLocation.search;

		if (theLocation.port) {
			theHostName += ":" + theLocation.port;
		}

		return theLocation.protocol + "//" + theHostName;
	};

	// Sends information about relevance of the activity to the server
	function sendRelevance(activityId, relevant){
		var prefixUrl = pageBaseUrl(location);
		var relevance = {"userId": eXo.env.portal.userName, "activityId": activityId, "relevant":relevant};
		$.ajax({
                url: prefixUrl + "/portal/rest/datacollector/collector",
                type: 'post',
                contentType: 'application/json',
                data: JSON.stringify(relevance)
            });
	}

})($);