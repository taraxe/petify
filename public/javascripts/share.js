/**
 * Created by IntelliJ IDEA.
 * User: alabbe
 * Date: 07/02/12
 * Time: 18:10
 * To change this template use File | Settings | File Templates.
 */

$(function(){
    $('.facebook').click(function(){
        $.social("publish",{text:"test"})
    });
    $('.twitter').click(function(){
        $.social("tweet",{text:encodeURI(message)})
    });
    /*$('.linkedin').click(function(){

    });
    $('.gmail').click(function(){

    });*/
});