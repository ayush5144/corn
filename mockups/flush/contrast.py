def lum(h):
    h=h.lstrip('#'); c=[int(h[i:i+2],16)/255 for i in (0,2,4)]
    c=[x/12.92 if x<=0.03928 else ((x+0.055)/1.055)**2.4 for x in c]
    return 0.2126*c[0]+0.7152*c[1]+0.0722*c[2]
def cr(a,b):
    la,lb=sorted([lum(a),lum(b)],reverse=True); return (la+0.05)/(lb+0.05)
T={'dark':dict(Bg='#071226',Surface='#0D1C36',Surface2='#142749',Line='#24395F',Text='#EAF4FF',Muted='#93A8C6',Accent='#3FD8FF',Fill='#1EA7FF',Ink='#03142B',Success='#3DDC97'),
   'light':dict(Bg='#F5FAFF',Surface='#E8F1FC',Surface2='#DAE7F7',Line='#C3D5EC',Text='#0A1A30',Muted='#4D6280',Accent='#005FCC',Fill='#1EA7FF',Ink='#03142B',Success='#11804E')}
for n,t in T.items():
    for a,b in [('Text','Bg'),('Muted','Bg'),('Accent','Bg'),('Ink','Fill'),('Success','Bg'),('Text','Surface'),('Muted','Surface2')]:
        r=cr(t[a],t[b]); print(n,a,b,round(r,2),'PASS' if r>=4.5 else 'FAIL')
