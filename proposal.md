#This is my Project Chatroom for class IT-114 Fall Semester 2024
#My name is Rank Kush 
#UCID : kr553
#Date is 12/11/2024

This project is for Chatroom application. It is a simple text-based chat application that allows users to send and receive messages. It have numerous functionalities such as users can roll a dice or coin for a little game, or user can send private messages to other users. The application also have a feature to display the chat history and download it to your local terminal. The application have feature to mute or unmute the client and UI will also have been dynamically updated. 


here are the links to my github pull requests that were divided into 4 milestones
-> https://github.com/KushDev19/kr553-IT114-005/pull/6 Mile stone 1
-> https://github.com/KushDev19/kr553-IT114-005/pull/7 Mile stone 2
-> https://github.com/KushDev19/kr553-IT114-005/pull/8 Mile stone 3
-> https://github.com/KushDev19/kr553-IT114-005/pull/9 Mile stone 4

*-> About the milestones->

**Milestone 1**:
-It was the initialization of the project which was readily available code tho we have to understand it with  detailed knowledge. Server can be started with terminal along side with client terminal. client can connect through '/connect' command and can initiate name via '/name' command and connect using localhost:3000 in the terminal when server is started.
-Server can be connect multiple clients and have concept of room. first room to be connected named lobby
-client can create new room with "/createroom" command and join room with "/joinroom" command.
-Clients can disconnect and reconnect without crashing the Server. 

**Milestone2**:
-Base Payload class for applicable messages/actions
-RollPayload class for the roll commands (below)
-creating client-side commands that can be used to roll a coin by '/roll 100' giving random values out of 100 or can be rolling a dice with '/roll 2d6' which gives addition of 2 dices rolled together and final random output with that command and '/flip' command flip a coin which either gives heads or tails based on randomization of outcomes.
-here learned and utilize how to make diffrent types of format in the terminal with inputs like  This is **bold** and *italic* along with _underlined_ with colored text such as #r red r#, #b blue b#, and #g green g# and must support a combination of all formats + color like **_*#r this is bold, underline, italic, and red r#*_**. gives output like --> This is <b>bold</b> and <i>italic</i> along with <u>underlined</u> with colored text such as <red>red</red>, <blue>blue</blue>, and <green>green</green> and must support a combination of all formats + color like <b><u><i><red> this is bold, underline, italic, and red </red></i></u></b>. in HTML format. 

**Milestone3**:
-creating UI panel to connect to the chatroom after providing values for username localhost 3000 they can connect to the chatroom and can send messages to the chatroom.  
-here we created UI panel for Chatroom too which have userlist, chatlog, input feild and send button to ultimately show into the Chat log.
-to preserve the format from milestone 2 there was some problems like UI was showing HTML tags and sometimes colors were not working correctly in the UI ultimately with the help of Internet I was able to solve the problem and UI was working perfectly fine.
-results from flip and roll appear in a different format then regular chat. 
-here we initiated the concept of private messages between users by tagging them like "@kush This is a private message" . it will not show to anyother user in the chatroom except the user tagged
-we also implemented the feature of mute and unmuting the user in the chatroom. when user is muted they will send messages but the one who muted them will not see the messages. when user is unmuted they will send messages and the one who unmuted them will see the messages. this will also be slash command like '/mute kush' or '/unmute kush'


**Milestone4**:
-creating a feature where user can export it's chat history to it's local terminal. this feature was implemented by creating a new button in the UI panel 'Export Chat' which when clicked will download the chat log of the file as it was shown in the UI.
-updating mute/unmute functionality where when one mutes another user, the user will be notified that one has muted the user, and it will also generate mute files with the name of the user itself to save muted info for the users in the server. 
-last feature that i tried to work on is highlighting the latest message in the UI. 
