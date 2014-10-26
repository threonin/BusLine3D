busLine3D
=========

Silly litte bus driving simulator I made as demo for HTL Pinkafeld. This is the second version as the first one was 2D and didn't allow driving the bus, only demo mode.
A video with 9 computers in a LAN is here, sorry for the bad quality: https://www.youtube.com/watch?v=2TSijPbiGz8#t=2m21s

It uses JMonkey-Engine and has network support.
The host controls the bus and the clients represent a bus station, you can upload a picture there which will be used as a passenger.
In single-player mode it uses predefined stations and passenger-pictures from a directory, I just used some pictures of politicians from Wikipedia, some of them are not in office anymore since then, but meh.

The bus was modeled after this clipart with blender: https://openclipart.org/detail/16199/yellow-bus-by-bobocal-16199
The sun is from here: https://openclipart.org/detail/1212/sol-de-mayo-bandera-de-argentina-by-liftarn

It can be controled with the arrow keys, space is the brake.
Physics debug information can be displayed with P and frame rate with I.
Enter enables demo mode, the bus location will be reset to the road, it will drive automatically and stop at every station. To leave demo mode simply steer the bus in any direction.
By dragging with the mouse you can change the view and with the mousewheel alter view distance.
When you drive near a station passengers will randomly enter or leave the bus, you can also topple trees/stations/signs.
The world is a disc and protected by a invisible dome so the bus cannot fall down.
When a client joins the circular road will increase in circumference and trees and houses placed randomly, the new station will be placed with a random distance.
