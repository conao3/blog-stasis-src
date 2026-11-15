import argparse
import datetime as dt
import json
import pathlib
from zoneinfo import ZoneInfo

import matplotlib

matplotlib.use('Agg')
import matplotlib.dates as mdates
import matplotlib.pyplot as plt


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('data', type=pathlib.Path)
    parser.add_argument('--prefix', type=pathlib.Path, required=True)
    args = parser.parse_args()
    data = json.loads(args.data.read_text())
    tz = ZoneInfo(data['timezone'])

    def stamp(s):
        return dt.datetime.fromisoformat(s.replace('Z', '+00:00')).astimezone(tz)

    start = stamp('2026-11-14T14:20:00Z')
    end = stamp(data['until'])
    rows = data['rows']
    palette = ['#1768ac', '#bb4430', '#2b7a4b', '#7d5796', '#b07c12']
    plt.rcParams.update({'font.size': 10, 'svg.fonttype': 'none', 'axes.spines.top': False,
                         'axes.spines.right': False, 'figure.facecolor': 'white',
                         'axes.labelcolor': '#333333', 'text.color': '#222222'})

    def save(fig, suffix):
        fig.savefig(str(args.prefix) + '-' + suffix + '.svg', bbox_inches='tight', metadata={'Date': None})
        fig.savefig(str(args.prefix) + '-' + suffix + '.png', bbox_inches='tight', dpi=150)
        plt.close(fig)

    fig, ax = plt.subplots(figsize=(13, 8.5))
    for i, row in enumerate(rows):
        color = palette[0 if row['label'] == 'Main' else 1 if row['label'] == 'Meta' else 2]
        for run in row['runs']:
            a, b = mdates.date2num(stamp(run['start'])), mdates.date2num(stamp(run['end']))
            ax.broken_barh([(a, max(b - a, 1 / 86400))], (i - .28, .56), facecolors=color, alpha=.6)
        points = row['points']
        ax.scatter([stamp(p['at']) for p in points], [i] * len(points), s=4, color=color, zorder=3)
        for c in row['compactions']:
            ax.scatter(stamp(c['end']), i, marker='D', color='#111111', s=24, zorder=4)
    ax.set_yticks(range(len(rows)), [r['label'] for r in rows])
    ax.invert_yaxis()
    ax.set_xlim(start, end)
    ax.xaxis.set_major_locator(mdates.HourLocator(tz=tz))
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M', tz=tz))
    ax.set_xlabel('2026-11-14 Toronto (EST)')
    ax.set_title('20 threads across research, implementation, production and event support', loc='left', pad=25)
    ax.grid(axis='x', alpha=.15)
    ax.text(0, 1.025, 'Bars: T3 run intervals (including waits)   Dots: model responses   Diamonds: 14 completed compactions',
            transform=ax.transAxes, fontsize=9)
    fig.tight_layout()
    save(fig, 'activity')

    threads = list(rows)
    priority = {'Main': 0, 'Meta': 1, 'payproof_implementation': 2, 'video_tools': 3, 'pitch_deck': 4}
    threads.sort(key=lambda r: priority.get(r['label'], 5))
    fig, axes = plt.subplots(7, 3, figsize=(13, 13), sharex=True, sharey=True)
    for ax, row in zip(axes.flat, threads):
        points = row['points']
        ax.plot([stamp(p['at']) for p in points], [p['input'] / 1000 for p in points],
                color=palette[0 if row['label'] == 'Main' else 1 if row['label'] == 'Meta' else 2], lw=1.2)
        for c in row['compactions']:
            ax.axvline(stamp(c['end']), color='#111111', ls=':', lw=1)
        ax.set_title(row['label'], loc='left', fontsize=10)
        ax.set_ylim(0, 475)
        ax.set_yticks([0, 250, 475])
        ax.set_xlim(start, end)
        ax.grid(alpha=.12)
        ax.xaxis.set_major_locator(mdates.HourLocator(byhour=[10, 14, 18], tz=tz))
        ax.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M', tz=tz))
        ax.tick_params(labelbottom=True, labelsize=8)
    for ax in list(axes.flat)[len(threads):]:
        ax.set_visible(False)
    fig.suptitle('Request input size for each thread', x=.06, ha='left', fontsize=16)
    fig.text(.06, .952, 'Thousands of input tokens per response; dotted lines mark completed compactions. Not cumulative usage.', fontsize=10)
    fig.supylabel('Input tokens (thousands)', fontsize=11)
    fig.supxlabel('Toronto (EST)', fontsize=11)
    fig.tight_layout(rect=[.02, .02, 1, .94])
    save(fig, 'context')

    major = sorted(rows, key=lambda r: r['usage'].get('input_tokens', 0), reverse=True)
    fig, ax = plt.subplots(figsize=(12, 7))
    cached = [r['usage'].get('cached_input_tokens', 0) / 1e6 for r in major]
    fresh = [(r['usage'].get('input_tokens', 0) - r['usage'].get('cached_input_tokens', 0)) / 1e6 for r in major]
    ax.barh(range(len(major)), cached, label='Cached input', color='#90b8d8')
    ax.barh(range(len(major)), fresh, left=cached, label='Other input (including cache creation)', color='#bb4430')
    ax.set_yticks(range(len(major)), [r['label'] for r in major])
    ax.invert_yaxis()
    ax.set_xlabel('Cumulative request input tokens (millions; repeated context included)')
    ax.set_title('911.4M input tokens, 98.85% cached input', loc='left')
    ax.legend(loc='lower right', frameon=False)
    ax.grid(axis='x', alpha=.12)
    fig.tight_layout()
    save(fig, 'usage')


if __name__ == '__main__':
    main()
