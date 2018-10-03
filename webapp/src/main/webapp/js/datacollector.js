(function($) {
	"use strict";
	
	$(document).ready(function() {
	 
		$( ".actionBar" ).find( ".statusAction" ).prepend( "<li class='eye eye-default'></li>");

		$('.eye').click(function() {

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

		});
		
})($);