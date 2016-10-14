<html xmlns="http://www.w3.org/TR/REC-html40">

<head>
	<meta http-equiv=Content-Type content="text/html; charset=windows-1252"></meta>
	<title>Feed Web Log</title>
</head>

<body lang=EN-US style='tab-interval: .5in'>


		
<h1>Feed Web Log Records 2</h1>
<hr></hr>

		<% if (request.getParameter("buttonName") != null) { %>
		You clicked
		<%= request.getParameter("buttonName") %>
		<% } else { %>
		No Click
		<% } %>

		<table class=MsoTableGrid border=1 
			style='border-collapse: collapse; border: none; 
			mso-border-alt: solid windowtext .5pt; mso-yfti-tbllook: 1184; 
			mso-padding-alt: 0in 5.4pt 0in 5.4pt'>
			<tr style='mso-yfti-irow: 0;  height: 57.1pt'>
				<td width=308 valign=top
					style='width: 185.0pt; border: solid windowtext 1.0pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<h2 style='line-height: normal'>Send One</h2>
				</td>
				<td width=353 valign=top
					style='width: 211.7pt; border: solid windowtext 1.0pt; border-left: none; mso-border-left-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<p class=MsoNormal
						style='margin-bottom: 0in; margin-bottom: .0001pt; line-height: normal'>
						&nbsp;
					</p> 
					<FORM NAME="Form_SendOne" METHOD="POST">
						<INPUT TYPE="HIDDEN" NAME="buttonName"></INPUT> <INPUT
							TYPE="BUTTON" VALUE="Send One" ONCLICK="Func_SendOne()"></INPUT>
					</FORM>

				</td>
			</tr>
			<tr style='mso-yfti-irow: 1; height: 57.1pt'>
				<td width=308 valign=top
					style='width: 185.0pt; border: solid windowtext 1.0pt; border-top: none; mso-border-top-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<h2 style='line-height: normal'>Send Slow</h2>
				</td>
				<td width=353 valign=top
					style='width: 211.7pt; border-top: none; border-left: none; border-bottom: solid windowtext 1.0pt; border-right: solid windowtext 1.0pt; mso-border-top-alt: solid windowtext .5pt; mso-border-left-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<p class=MsoNormal
						style='margin-bottom: 0in; margin-bottom: .0001pt; line-height: normal'>
						&nbsp;
					</p>
					<FORM NAME="Form_SendSlow" METHOD="POST">
						<INPUT TYPE="HIDDEN" NAME="buttonName"></INPUT> 
						<INPUT TYPE="BUTTON" VALUE="Send Slow" ONCLICK="Func_SendSlow()"></INPUT>
					</FORM>
				</td>
			</tr>
			<tr style='mso-yfti-irow: 2; height: 57.1pt'>
				<td width=308 valign=top
					style='width: 185.0pt; border: solid windowtext 1.0pt; border-top: none; mso-border-top-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<h2 style='line-height: normal'>Send Fast</h2>
				</td>
				<td width=353 valign=top
					style='width: 211.7pt; border-top: none; border-left: none; border-bottom: solid windowtext 1.0pt; border-right: solid windowtext 1.0pt; mso-border-top-alt: solid windowtext .5pt; mso-border-left-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<p class=MsoNormal
						style='margin-bottom: 0in; margin-bottom: .0001pt; line-height: normal'>
						&nbsp;
					</p>
					<FORM NAME="Form_SendFast" METHOD="POST">
						<INPUT TYPE="HIDDEN" NAME="buttonName"></INPUT> 
						<INPUT
							TYPE="BUTTON" VALUE="Send Fast" ONCLICK="Func_SendFast()"></INPUT>
					</FORM>
				</td>
			</tr>
			<tr style='mso-yfti-irow: 3; height: 57.1pt'>
				<td width=308 valign=top
					style='width: 185.0pt; border: solid windowtext 1.0pt; border-top: none; mso-border-top-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<h2 style='line-height: normal'>Send Very Fast</h2>
				</td>
				<td width=353 valign=top
					style='width: 211.7pt; border-top: none; border-left: none; border-bottom: solid windowtext 1.0pt; border-right: solid windowtext 1.0pt; mso-border-top-alt: solid windowtext .5pt; mso-border-left-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<p class=MsoNormal
						style='margin-bottom: 0in; margin-bottom: .0001pt; line-height: normal'>
						&nbsp;
					</p>
					<FORM NAME="Form_SendVeryFast" METHOD="POST">
						<INPUT TYPE="HIDDEN" NAME="buttonName"></INPUT>
						 <INPUT
							TYPE="BUTTON" VALUE="Send Very Fast" ONCLICK="Func_SendVeryFast()"></INPUT>
					</FORM>
				</td>
			</tr>
			<tr style='mso-yfti-irow: 4; mso-yfti-lastrow: yes; height: 57.1pt'>
				<td width=308 valign=top
					style='width: 185.0pt; border: solid windowtext 1.0pt; border-top: none; mso-border-top-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<h2 style='line-height: normal; mso-outline-level: 1'>Send
						Custom</h2>
				</td>
				<td width=353 valign=top
					style='width: 211.7pt; border-top: none; border-left: none; border-bottom: solid windowtext 1.0pt; border-right: solid windowtext 1.0pt; mso-border-top-alt: solid windowtext .5pt; mso-border-left-alt: solid windowtext .5pt; mso-border-alt: solid windowtext .5pt; padding: 0in 5.4pt 0in 5.4pt; height: 57.1pt'>
					<p class=MsoNormal
						style='margin-bottom: 0in; margin-bottom: .0001pt; line-height: normal'>
						&nbsp;
					</p>
					<FORM NAME="Form_SendCustom" METHOD="POST">
						<INPUT TYPE="HIDDEN" NAME="buttonName"></INPUT>
						<INPUT TYPE="BUTTON" VALUE="Send Custom" ONCLICK="Func_SendCustom()"></INPUT>
						<br></br>
						Custom Value: <INPUT TYPE="text" NAME="custom_value" ></INPUT><br></br>
					</FORM>
				</td>
			</tr>
		</table>
	<% if(request.getParameter("buttonName") != null) { %>
	You clicked
	<%= request.getParameter("buttonName") %>
	<br></br>
	<% if (request.getParameter("custom_value") != null) { %>
	With Value
	<%= request.getParameter("custom_value") %>
	<%	} %>
    <%  } %>
<hr></hr>

	<div
		style='mso-element: para-border-div; border: none; border-bottom: solid #4F81BD 1.0pt; mso-border-bottom-themecolor: accent1; padding: 0in 0in 4.0pt 0in'>

		<p class=MsoTitle>
			&nbsp;
		</p>

	</div>



	<SCRIPT type="text/javascript">
	<!--
		function Func_SendOne() {
			document.Form_SendOne.buttonName.value = "Send One"
			Form_SendOne.submit()
		}
		function Func_SendSlow() {
			document.Form_SendSlow.buttonName.value = "Send Slow"
			Form_SendSlow.submit()
		}
		function Func_SendFast() {
			document.Form_SendFast.buttonName.value = "Send Fast"
			Form_SendFast.submit()
		}
		function Func_SendVeryFast() {
			document.Form_SendVeryFast.buttonName.value = "Send Very Fast"
			Form_SendVeryFast.submit()
		}
		function Func_SendCustom() {
			document.Form_SendCustom.buttonName.value = "Send Custom"
			Form_SendCustom.submit()
		}
	// -->
	</SCRIPT>

</body>

</html>
									