# message-renderer-template

[![Build Status](https://travis-ci.org/hmrc/message-renderer-template.svg)](https://travis-ci.org/hmrc/message-renderer-template) [ ![Download](https://api.bintray.com/packages/hmrc/releases/message-renderer-template/images/download.svg) ](https://bintray.com/hmrc/releases/message-renderer-template/_latestVersion)

Micro-service responsible for:

- on-demand creation of generated messages with a chosen tax identifier
- proving that message and other services' integration with chosen tax identifiers works

Acts as an example application for other teams to create their own renderers.

## API

| Path                                              | Supported Methods | Description                                                                                                               |
| ------------------------------------------------- | ----------------  | ------------------------------------------------------------------------------------------------------------------------  |
| ```/messages```                                   | POST              | Create a new message body and header [More...](#post-messages)    |                                              
| ```/messages/:id```                               | GET               | Get message body by id [More...](#get-messagesid) |

## Endpoints

### POST /messages

Create a new message. Will create both:
 - message header - stored in message service, used to display on the list, contains subject and some metadata information
 - message body - stored in message renderer template itself as a raw html body (*Note: renderers are free to store information needed to render the message as they see fit*)

Example Self Assessment message creation request for saUtr:

```json
  {
  		"regime" : "sa",
  		"taxId" : {
  			"name": "sautr",
        "value": "1234567899"
  	 },
    "statutory" : true
  } 
```

Example message creation request for nino:

```json
  {
  		"regime" : "paye",
  		"taxId" : {
  			"name": "nino",
        "value" : "QQ123456C"
  		}
  } 
```

**Note**: 
* statutory has meaning for Self Assessment message creation only.

Responds with status code:

- 201 if the message is successfully created
- 200 if the message has already been created before (currently relies on 409 response from message, meaning that message with the same 'hash' has been stored before by the message service)

**Note**: 
At the moment 'hash' is generated based on date/time so the probablility of a clash is very low. Renderers can compute their hashes as they see fit. 

### GET /messages/:id

Returns message body by id.

Example response for message that has been read:

```html
<h2>Message for recipient from paye with nino equal to AA055123C</h2> 
<div>Created at 2016-10-05T15:35:41.973Z</div> 
<div>This is a message that has been generated for user with nino value of AA055123C.</div>
```

Responds with status code:
 - 200 if the message body with a given id exists
 - 404 if it doesn't exist

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
