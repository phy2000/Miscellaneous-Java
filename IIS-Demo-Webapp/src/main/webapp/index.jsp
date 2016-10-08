<HTML>
    <HEAD>
        <TITLE>Using Buttons</TITLE>
    </HEAD>

    <BODY>
        <% 
            //if(request.getParameter("buttonName") != null) {
            if(request.getParameterNames() != null) {
        %>
            You clicked 
            <%= request.getParameter("buttonName") %>
        <%
            }
        %>

        <FORM NAME="formblah" METHOD="POST">
            <INPUT TYPE="HIDDEN" NAME="buttonName">
            <INPUT TYPE="BUTTON" VALUE="Button 1" ONCLICK="button1()">
            <INPUT TYPE="BUTTON" VALUE="Button 2" ONCLICK="button2()">
        </FORM>

        <SCRIPT LANGUAGE="JavaScript">
            <!--
            function button1()
            {
                document.formblah.buttonName.value = "button blah"
                	formblah.submit()
            }    
            function button2()
            {
                document.formblah.buttonName.value = "button blah blah"
                	formblah.submit()
            }    
            // --> 
        </SCRIPT>
    </BODY>
</HTML>