(function($) {
	"use strict";
	
	$(document).ready(function() {
	 
	 	// Add default eye icons
		$( ".actionBar" ).find( ".statusAction" ).prepend( "<li class='eye eye-default'></li>");

		
		// Listener for changing icon state
		addEyeOnClickListener();

		// Observe changes in the stream to add icon to new activities
		var observer = new MutationObserver(function(mutations) {
		  mutations.forEach(function(mutation) {
		  	
		    if(mutation.addedNodes.length > 3){
		    	$(mutation.target).find(".statusAction").prepend("<li class='eye eye-default'></li>");
		    	addEyeOnClickListener();
		    }
		  });    
		});
		 
		observer.observe($("#UIUserActivitiesDisplay").get(0), {childList: true, subtree: true});

		});

	function addEyeOnClickListener() {
		$(".eye").click(function() {
			var link = $(this).parents(".boxContainer").find('.heading > .actLink > a')[0];
			var activityId = $(link).attr("href").substring($(link).attr("href").indexOf('=') + 1);
			
			if($(this).hasClass("eye-default")){
				console.log("Action: relevant | ID: " + activityId);
				$(this).removeClass("eye-default");
				$(this).toggleClass('eye-relevant');
			}
			else if($(this).hasClass("eye-relevant")){
				
				console.log("Action: irrelevant | ID: " + activityId);
				$(this).removeClass("eye-relevant");
				$(this).toggleClass('eye-irrelevant');
			}
			else{
				console.log("Action: relevant | ID: " + activityId);
				$(this).removeClass("eye-irrelevant");
				$(this).toggleClass('eye-relevant');
			}
		});
	}

})($);