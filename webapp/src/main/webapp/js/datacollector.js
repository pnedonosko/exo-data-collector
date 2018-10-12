(function($) {
	"use strict";

	$(document).ready(function() {

 	// Set initial state of the icons
 	updateStateOfIcons($( ".boxContainer" ));

	// Observe changes in the stream to add icons to new activities
	var observer = new MutationObserver(function(mutations) {
		mutations.forEach(function(mutation) {
			if(mutation.addedNodes.length > 3){
				updateStateOfIcons($(mutation.target));
			}
		});    
	});
	
	// Start observing 
	observer.observe($("#UIUserActivitiesDisplay").get(0), {childList: true, subtree: true});
	
	// If there is no activities in the stream, creating a new activity causes recreating the target of observer	
	$("#ShareButton").click(function(){
		// Wait for creating new structore of stream and start observing
		setTimeout(function(){
			observer.observe($("#UIUserActivitiesDisplay").get(0), {childList: true, subtree: true});
			updateStateOfIcons($( ".boxContainer" ));
		}, 1000);
	});
});


// Adds onClick listener to the elements
function addRelevanceOnClickListener(elements) {
	$(elements).click(function() {
		if($(this).attr("onClick") == undefined){
			// The link contains activityId
			var link = $(this).parents(".boxContainer").find('.heading > .actLink > a')[0];
			var activityId = $(link).attr("href").substring($(link).attr("href").indexOf('=') + 1);
			
			if($(this).hasClass("relevance-default")){
				console.log("Action: relevant | ID: " + activityId);
				sendRelevance(activityId, true);
				$(this).removeClass("relevance-default");
				$(this).toggleClass('relevance-relevant');
				$(this).toggleClass('uiIconBlue');
			}
			else if($(this).hasClass("relevance-relevant")){
				
				console.log("Action: irrelevant | ID: " + activityId);
				sendRelevance(activityId, false);
				$(this).removeClass("relevance-relevant");
				$(this).toggleClass('relevance-irrelevant');
			}
			else{
				console.log("Action: relevant | ID: " + activityId);
				sendRelevance(activityId, true);
				$(this).removeClass("relevance-irrelevant");
				$(this).toggleClass('relevance-relevant');
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
	// The object of relevance to be sent to the server
	var relevance = {"userId": eXo.env.portal.userName, "activityId": activityId, "relevant":relevant};

	$.ajax({
		url: prefixUrl + "/portal/rest/datacollector/collector",
		type: 'post',
		contentType: 'application/json',
		data: JSON.stringify(relevance)
	});
}

// Updates state of the icons. Accepts any parent div of an icon element
function updateStateOfIcons(iconsParentDiv){

	var prefixUrl = pageBaseUrl(location);

	// Iterates through each activity block and inserts the relevance icon
	$(iconsParentDiv).find('.actionBar > .statusAction').each(function(){
		// The link contains activityId
		var link = $(this).parents(".boxContainer").find('.heading > .actLink > a')[0];
		var activityId = $(link).attr("href").substring($(link).attr("href").indexOf('=') + 1);
		var userId = eXo.env.portal.userName

		// To be used in ajax success/error function
		var current = $(this);

		// If there is already icon move to the next block
		if($(this).find('li.relevance').length !== 0){
			return;
		}

		$.ajax({
			url: prefixUrl + "/portal/rest/datacollector/collector/" + userId + "/" + activityId,
			type: 'get',

			success: function (data){
				if(data.relevant){
					$(current).prepend( "<li class='relevance relevance-relevant uiIconBlue'></li>");	
				}
				else{
					$(current).prepend( "<li class='relevance relevance-irrelevant uiIconBlue'></li>");	
				}

            	// Add onClick listener to new icon
            	addRelevanceOnClickListener($(current).find('.relevance'));
            },

            error: function(XMLHttpRequest){
            	 // If user hasn't checked relevance for the activity
            	 if(XMLHttpRequest.status == 404){
            	 	// Add default icon
            	 	$(current).prepend( "<li class='relevance relevance-default'></li>");
            	 	// Add onClickListener to new icon
            	 	addRelevanceOnClickListener($(current).find('.relevance'));
            	 } else{
            	 	console.log('Data Collector: Error status: ' + XMLHttpRequest.status + ', text: ' + XMLHttpRequest.statusText);
            	 }
            	}

            });	
	});	
}

})($);
