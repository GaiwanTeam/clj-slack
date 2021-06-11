grammar markdown;

source: token+ ;

token: italic | bold | undecorated ;

italic: '_' token* '_' ;

bold: '*' token* '*' ;

undecorated: 'b'+ ;
