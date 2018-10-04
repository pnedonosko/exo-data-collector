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

			if($(this).hasClass("eye-default")){
				$(this).removeClass("eye-default");
				$(this).toggleClass('eye-relevant');
			}
			else if($(this).hasClass("eye-relevant")){
				$(this).removeClass("eye-relevant");
				$(this).toggleClass('eye-irrelevant');
			}
			else{
				$(this).removeClass("eye-irrelevant");
				$(this).toggleClass('eye-relevant');
			}
		});
	}

})($);