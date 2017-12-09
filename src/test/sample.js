/*
 Copyright (C) 2017 Ravinder Krishnaswamy

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice 
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH 
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, 
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS 
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER 
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
THIS SOFTWARE.
*/


function contact_address_update_trigger(Contact, Go_Post_Insert, Go_Post_Update)
{
    var nonNullContacts = {};
    var acctsForAddress = [];

    var ctx = GoV8.getObject('goTriggerContext');
    if (ctx.isInsert()) {
        for (var c in c.new()) {
            if (c.getField('Mailing_Address_Line1') != "" && c.getField('Account') != null) {
                nonNullContacts[c.getField('Id')] = c; // type GoId must implement equality
                acctsForAddress.add(c.getField('Account'));
            }
        }
    } else if (ctx.isUpdate()) {
    }

    var accts = GoV8.getReferencedObjects('Account', [ID], 'Contact');
    var acctAddrMap = {};

    for (var a in accts) {
        acctAddrMap[a.getField('Id')] = a;
    }

    var cupdatelist = [];
    for (var ckey in nonNullContacts) {
        var acct = acctAddrMap[nonNullContacts[ckey].getField('Account')];
        nonNullContacts[ckey].setField('Mailing_Address_Line_1', acct.getField('Billing_Address_Line_1'));
        // etc.
        // ...
        cupdatelist.add(nonNullContacts[ckey]);
    }

    GoV8.getObject('goTransaction').update(cupdatelist);



}
