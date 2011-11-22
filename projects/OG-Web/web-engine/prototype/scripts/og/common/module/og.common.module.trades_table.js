/*
 * @copyright 2009 - present by OpenGamma Inc
 * @license See distribution for license
 */
$.register_module({
    name: 'og.common.module.trade_table',
    dependencies: [],
    obj: function () {
        var table = '\
          <table class="OG-table">\
            <thead>\
              <tr>\
                <th><span>Trades</span></th>\
                <th>Quality</th>\
                <th>Counterparty</th>\
                <th>Date</th>\
              </tr>\
            </thead>\
            <tbody>{TBODY}</tbody>\
          </table>\
        ',
        attributes = '\
          <tr class="og-js-attribute" style="display: none">\
            <td colspan="4" style="padding-left: 15px">\
              <table class="og-sub-list">{TBODY}</table>\
            </td>\
          </tr>\
        ',
        sub_head = '<tbody><tr><td class="og-header" colspan="2">{ATTRIBUTES}</td></tr></tbody>',
        disable_expand = function (config) {$(config.selector + ' .og-icon-expand').hide()};
        return function (config) {
            var trades = config.trades, selector = config.selector, tbody, has_attributes = false,
                fields = ['id', 'quantity', 'counterParty', 'date'], start = '<tr><td>', end = '</td></tr>';
            if (!trades[0]) return $(selector).html(table.replace('{TBODY}', '<tr><td colspan="4">No Trades</td></tr>'));
            tbody = trades.reduce(function (acc, trade) {
                acc.push(start, fields.map(function (field, i) {
                    var expander;
                    i === 0 ? expander = '<span class="OG-icon og-icon-expand"></span>' : expander = '';
                    return expander + trade[field].replace(/.*~/, '');
                }).join('</td><td>'), end);
                (function () { // display attributes if available
                    var attr, attr_type, attr_obj, key, html = [];
                    if (!Object.keys(trade.attributes).length) return;
                    for (attr_type in trade.attributes) {
                        attr_obj = trade.attributes[attr_type], attr = [];
                        if (!Object.keys(attr_obj).length) continue;
                        for (key in attr_obj) attr.push(
                            start, key.replace(/.+~(.+)/, '$1').lang(), ':</td><td>', attr_obj[key].lang(), end
                        );
                        html.push(
                            sub_head.replace('{ATTRIBUTES}', attr_type.lang()) +
                            '<tbody class="OG-background-01">' + attr.join('') + '</tbody>'
                        );
                    }
                    acc.push(attributes.replace('{TBODY}', html.join('')));
                    if (html.length) has_attributes = true;
                }());
                return acc;
            }, []).join('');
            $(selector).html(table.replace('{TBODY}', tbody));
            if (!has_attributes) disable_expand(config); // remove all expand links if no attributes
            $(selector + ' .OG-table > tbody > tr').each(function () { // remove expand links that have no attributes
                var $this = $(this);
                if ($this.next().hasClass('og-js-attribute')) {
                    $this.find('.og-icon-expand').bind('click', function () {
                        $(this).toggleClass('og-icon-collapse').parents('tr').next().toggle();
                    });
                } else $this.find('.og-icon-expand').css('visibility', 'hidden');
            });
            $(selector + ' .OG-table').awesometable({height: 400});
        }
    }
});