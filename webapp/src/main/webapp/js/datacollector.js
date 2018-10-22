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

				$(this).closest('a.relevance-tooltip').attr("data-original-title", "Irrelevant");
			}
			else if($(this).hasClass("relevance-relevant")){
				
				console.log("Action: irrelevant | ID: " + activityId);
				sendRelevance(activityId, false);
				$(this).removeClass("relevance-relevant");
				$(this).toggleClass('relevance-irrelevant');

				$(this).closest('a.relevance-tooltip').attr("data-original-title", "Relevant");
			}
			else{
				console.log("Action: relevant | ID: " + activityId);
				sendRelevance(activityId, true);
				$(this).removeClass("relevance-irrelevant");
				$(this).toggleClass('relevance-relevant');

				$(this).closest('a.relevance-tooltip').attr("data-original-title", "Irrelevant");
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
	// The object of relevance to be sent to the server
	var relevance = {"userId": eXo.env.portal.userName, "activityId": activityId, "relevant":relevant};
	postRelevance(relevance);
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
		if($(this).find('.relevance').length !== 0){
			return;
		}

		var promisedRelevance = getRelevance(userId, activityId);

		// If server responded with relevance
		promisedRelevance.done(function(data){
			if(data.relevant){
				$(current).prepend(getRelevantIcon());	
			}
			else{
				$(current).prepend(getIrrelevantIcon());	
			}
            	// Add onClick listener to new icon
            	addRelevanceOnClickListener($(current).find('.relevance'));
            	// Call tooltip handler
            	$(current).find('a.relevance-tooltip').tooltip();
            });

		// If server responded with error
		promisedRelevance.fail(function(XMLHttpRequest){
            // If user hasn't checked relevance for the activity
            if(XMLHttpRequest.status == 404){
            // Add default icon
            $(current).prepend(getDefaultIcon());
            	// Add onClickListener to new icon
            	addRelevanceOnClickListener($(current).find('.relevance'));
            } else{
            	console.log('Data Collector: Error status: ' + XMLHttpRequest.status + ', text: ' + XMLHttpRequest.statusText);
            }
            // Call tooltip handler
            $(current).find('a.relevance-tooltip').tooltip();
        });


	});	

}




var postRelevance = function(relevance) {
	var prefixUrl = pageBaseUrl(location);

	var request = $.ajax({
		url: prefixUrl + "/portal/rest/datacollector/collector",
		type: 'post',
		contentType: 'application/json',
		data: JSON.stringify(relevance)
	});
	return request;
};

var getRelevance = function(userId, activityId) {
	var prefixUrl = pageBaseUrl(location);

	var request = $.ajax({
		url: prefixUrl + "/portal/rest/datacollector/collector/" + userId + "/" + activityId,
		type: 'get'
	});
	return request;
};

var getRelevantIcon = function(){
	return '<li><a rel="tooltip" href="javascript:void(0);" class="relevance-tooltip" data-placement="bottom" data-original-title="Irrelevant"><span class="relevance relevance-relevant uiIconBlue"></span>&nbsp;&nbsp;&nbsp;</a></li>';
}

var getIrrelevantIcon = function(){
	return '<li><a rel="tooltip" href="javascript:void(0);" class="relevance-tooltip"  data-placement="bottom" data-original-title="Relevant"><span class="relevance relevance-irrelevant uiIconBlue"></span>&nbsp;&nbsp;&nbsp;&nbsp;</a></li>';
}

var getDefaultIcon = function(){
	return '<li><a rel="tooltip" href="javascript:void(0);" class="relevance-tooltip" data-placement="bottom" data-original-title="Relevant"><span class="relevance relevance-default"></span>&nbsp;&nbsp;&nbsp;</a></li>';
}
})($);
