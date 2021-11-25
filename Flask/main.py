from flask import Flask, redirect, render_template, request, session, url_for
import pandas as pd
# import json
# import random

# Flask Initialization
app = Flask(__name__)

@app.route("/")

@app.route("/login", methods=["POST"])
def login():
    if request.method == "POST":
        lusr = request.values['usr']
        lpwd = request.values['pwd']
        
        if lpwd == usr_data[usr_data['acc']==lusr]['pwd'].iloc[0]:
            session['usr'] = str(lusr)
            return redirect("/")
        else:
            return redirect("/")


@app.route("/register", methods=["POST"])
def register():
    if request.method == "POST":
        usr = request.values['rusr']
        pwd = request.values['rpwd']
        cnf = request.values['rpwd2']

        if str(pwd) == str(cnf):
            with db_reader.cursor() as cur:
            # Create a new record
                sql = "INSERT INTO `usr` (`acc`, `pwd`) VALUES (%s, %s)"
                cur.execute(sql, (usr, pwd))
                db_reader.commit()

            session['usr'] = str(usr)
            return redirect("/")


@app.route("/login")
def lock():
    return render_template("login.html")


@app.route("/logout")
def logout():
    session['usr'] = False
    return redirect("/")


@app.errorhandler(400) # Redirecting "Bad Request"
def bad_request(e):
    return redirect("/")


@app.errorhandler(404) # Redirecting undefined URLs
def page_not_found(e):
    return redirect("/")


@app.errorhandler(500) # Redirecting "Internal Server Error"
def internal_server_error(e):
    return redirect("/")


if __name__ == "__main__":
    app.run(debug=True, port=8001)